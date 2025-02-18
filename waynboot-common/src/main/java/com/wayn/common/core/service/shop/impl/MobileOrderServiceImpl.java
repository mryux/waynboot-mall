package com.wayn.common.core.service.shop.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wayn.common.config.WaynConfig;
import com.wayn.common.core.entity.shop.*;
import com.wayn.common.core.mapper.shop.OrderMapper;
import com.wayn.common.core.service.shop.*;
import com.wayn.common.core.vo.OrderDetailVO;
import com.wayn.common.core.vo.OrderGoodsVO;
import com.wayn.common.request.OrderCommitReqVO;
import com.wayn.common.response.OrderListDataResVO;
import com.wayn.common.response.OrderListResVO;
import com.wayn.common.response.OrderStatusCountResVO;
import com.wayn.common.response.SubmitOrderResVO;
import com.wayn.common.util.OrderHandleOption;
import com.wayn.common.util.OrderUtil;
import com.wayn.data.redis.manager.RedisCache;
import com.wayn.message.core.constant.MQConstants;
import com.wayn.message.core.dto.OrderDTO;
import com.wayn.util.constant.Constants;
import com.wayn.util.enums.OrderStatusEnum;
import com.wayn.util.enums.ReturnCodeEnum;
import com.wayn.util.exception.BusinessException;
import com.wayn.util.util.IdUtil;
import com.wayn.util.util.OrderSnGenUtil;
import com.wayn.util.util.bean.MyBeanUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.wayn.data.redis.constant.RedisKeyEnum.ORDER_RESULT_KEY;
import static com.wayn.util.constant.SysConstants.ORDER_SUBMIT_ERROR_MSG;

/**
 * 订单表 服务实现类
 *
 * @author wayn
 * @since 2020-08-11
 */
@Slf4j
@Service
@AllArgsConstructor
public class MobileOrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IMobileOrderService {

    private RedisCache redisCache;
    private IAddressService iAddressService;
    private ICartService iCartService;
    private IOrderGoodsService iOrderGoodsService;
    private IGoodsProductService iGoodsProductService;
    private IGoodsService iGoodsService;
    private OrderMapper orderMapper;
    private RabbitTemplate rabbitTemplate;
    private RabbitTemplate delayRabbitTemplate;
    private OrderSnGenUtil orderSnGenUtil;
    private ShopMemberCouponService shopMemberCouponService;
    private IOrderUnpaidService orderUnpaidService;

    @Override
    public OrderListResVO selectListPage(IPage<Order> page, Integer showType, Long userId) {
        List<Short> orderStatus = OrderUtil.orderStatus(showType);
        Order order = new Order();
        order.setUserId(userId);
        IPage<Order> orderIPage = orderMapper.selectOrderListPage(page, order, orderStatus);
        List<Order> orderList = orderIPage.getRecords();
        List<Long> idList = orderList.stream().map(Order::getId).collect(Collectors.toList());
        Map<Long, List<OrderGoods>> orderGoodsListMap = iOrderGoodsService
                .list(Wrappers.lambdaQuery(OrderGoods.class).in(CollectionUtils.isNotEmpty(idList), OrderGoods::getOrderId, idList))
                .stream().collect(Collectors.groupingBy(OrderGoods::getOrderId));
        List<OrderListDataResVO> dataList = new ArrayList<>();
        for (Order o : orderList) {
            OrderListDataResVO data = new OrderListDataResVO();
            data.setId(o.getId());
            data.setOrderSn(o.getOrderSn());
            data.setActualPrice(o.getActualPrice());
            data.setHandleOption(OrderUtil.build(o));
            data.setOrderStatusText(OrderUtil.orderStatusText(o));
            List<OrderGoods> orderGoodsList = orderGoodsListMap.get(o.getId());
            List<OrderGoodsVO> orderGoodsVOS = BeanUtil.copyToList(orderGoodsList, OrderGoodsVO.class);
            data.setGoodsList(orderGoodsVOS);
            dataList.add(data);
        }
        OrderListResVO resVO = new OrderListResVO();
        resVO.setData(dataList);
        resVO.setPage(orderIPage.getCurrent());
        resVO.setPages(orderIPage.getPages());
        return resVO;
    }

    @Override
    public OrderStatusCountResVO statusCount(Long userId) {
        List<Order> orderList = list(new QueryWrapper<Order>().select("order_status", "comments").eq("user_id", userId));
        long unpaid = 0;
        long unship = 0;
        long unrecv = 0;
        long uncomment = 0;
        for (Order order : orderList) {
            if (OrderUtil.isCreateStatus(order)) {
                unpaid++;
            } else if (OrderUtil.isPayStatus(order)) {
                unship++;
            } else if (OrderUtil.isShipStatus(order)) {
                unrecv++;
            } else if (OrderUtil.isConfirmStatus(order) || OrderUtil.isAutoConfirmStatus(order)) {
                uncomment += order.getComments();
            }

        }
        OrderStatusCountResVO resVO = new OrderStatusCountResVO();
        resVO.setUncomment(uncomment);
        resVO.setUnrecv(unrecv);
        resVO.setUnship(unship);
        resVO.setUnpaid(unpaid);
        return resVO;
    }

    @Override
    public OrderDetailVO getOrderDetailByOrderSn(String orderSn) {
        LambdaQueryWrapper<Order> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Order::getOrderSn, orderSn);
        Order order = getOne(queryWrapper);
        if (order == null) {
            throw new BusinessException(ReturnCodeEnum.ORDER_NOT_EXISTS_ERROR);
        }
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        MyBeanUtil.copyProperties(order, orderDetailVO);
        orderDetailVO.setOrderStatusText(OrderUtil.orderStatusText(order));
        orderDetailVO.setPayTypeText(OrderUtil.payTypeText(order));
        LambdaQueryWrapper<OrderGoods> queryWrapper1 = Wrappers.lambdaQuery(OrderGoods.class);
        queryWrapper1.eq(OrderGoods::getOrderId, order.getId());
        List<OrderGoods> list = iOrderGoodsService.list(queryWrapper1);
        List<OrderGoodsVO> orderGoodsVOS = BeanUtil.copyToList(list, OrderGoodsVO.class);
        orderDetailVO.setOrderGoodsVOList(orderGoodsVOS);
        return orderDetailVO;
    }

    @Override
    public SubmitOrderResVO asyncSubmit(OrderCommitReqVO orderCommitReqVO, Long userId) {
        OrderDTO orderDTO = new OrderDTO();
        MyBeanUtil.copyProperties(orderCommitReqVO, orderDTO);
        Long addressId = orderDTO.getAddressId();
        Long userCouponId = orderDTO.getUserCouponId();
        orderDTO.setUserId(userId);
        Address address = iAddressService.getById(addressId);
        if (!Objects.equals(address.getMemberId(), userId)) {
            throw new BusinessException(ReturnCodeEnum.ORDER_ERROR_ADDRESS_ERROR);
        }

        // 获取用户订单商品，为空默认取购物车已选中商品
        List<Long> cartIdArr = orderDTO.getCartIdArr();
        List<Cart> checkedGoodsList;
        if (CollectionUtils.isEmpty(cartIdArr)) {
            checkedGoodsList = iCartService.list(new QueryWrapper<Cart>().eq("checked", true).eq("user_id", userId));
        } else {
            checkedGoodsList = iCartService.listByIds(cartIdArr);
        }
        if (CollectionUtils.isEmpty(checkedGoodsList)) {
            throw new BusinessException(ReturnCodeEnum.ORDER_ERROR_CART_EMPTY_ERROR);
        }

        // 商品费用
        BigDecimal checkedGoodsPrice = BigDecimal.ZERO;
        for (Cart checkGoods : checkedGoodsList) {
            checkedGoodsPrice = checkedGoodsPrice.add(checkGoods.getPrice().multiply(new BigDecimal(checkGoods.getNumber())));
        }

        // 根据订单商品总价计算运费，满足条件（例如88元）则免运费，否则需要支付运费（例如8元）；
        BigDecimal freightPrice = BigDecimal.ZERO;
        if (checkedGoodsPrice.compareTo(WaynConfig.getFreightLimit()) < 0) {
            freightPrice = WaynConfig.getFreightPrice();
        }

        // 订单费用
        BigDecimal orderTotalPrice = checkedGoodsPrice.add(freightPrice);

        // 优惠卷抵扣费用
        BigDecimal couponPrice = BigDecimal.ZERO;
        if (userCouponId != null) {
            ShopMemberCoupon memberCoupon = shopMemberCouponService.getById(userCouponId);
            if (memberCoupon == null || memberCoupon.getUserId() != Math.toIntExact(userId)) {
                throw new BusinessException(ReturnCodeEnum.ORDER_SUBMIT_ERROR, "优惠卷错误");
            }
            if (memberCoupon.getUseStatus() != 0 || DateUtil.compare(memberCoupon.getExpireTime(), new Date()) < 0) {
                throw new BusinessException(ReturnCodeEnum.ORDER_SUBMIT_ERROR, "优惠卷不可用");
            }
            if (memberCoupon.getMin() > orderTotalPrice.intValue()) {
                throw new BusinessException(ReturnCodeEnum.ORDER_SUBMIT_ERROR, "优惠卷使用门槛未达到");
            }
            couponPrice = BigDecimal.valueOf(memberCoupon.getDiscount());
        }

        // 最终支付费用
        BigDecimal actualPrice = orderTotalPrice.subtract(couponPrice).max(BigDecimal.ZERO);
        ;
        String orderSn = orderSnGenUtil.generateOrderSn();
        orderDTO.setOrderSn(orderSn);

        // 异步下单
        String uid = IdUtil.getUid();
        CorrelationData correlationData = new CorrelationData(uid);
        Map<String, Object> map = new HashMap<>();
        map.put("order", orderDTO);
        map.put("notifyUrl", WaynConfig.getMobileUrl() + "/callback/order/submit");
        try {
            Message message = MessageBuilder
                    .withBody(JSON.toJSONString(map).getBytes(Constants.UTF_ENCODING))
                    .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.convertAndSend(MQConstants.ORDER_DIRECT_EXCHANGE, MQConstants.ORDER_DIRECT_ROUTING, message, correlationData);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }
        SubmitOrderResVO resVO = new SubmitOrderResVO();
        resVO.setActualPrice(actualPrice);
        resVO.setOrderSn(orderSn);
        return resVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(OrderDTO orderDTO) throws UnsupportedEncodingException {
        Long userId = orderDTO.getUserId();
        String orderSn = orderDTO.getOrderSn();
        Long userCouponId = orderDTO.getUserCouponId();
        // 获取用户地址
        Long addressId = orderDTO.getAddressId();
        Address checkedAddress;
        if (Objects.isNull(addressId)) {
            throw new BusinessException(ReturnCodeEnum.ORDER_ERROR_ADDRESS_ERROR);
        }
        checkedAddress = iAddressService.getById(addressId);

        // 获取用户订单商品，为空默认取购物车已选中商品
        List<Long> cartIdArr = orderDTO.getCartIdArr();
        List<Cart> checkedGoodsList;
        if (CollectionUtils.isEmpty(cartIdArr)) {
            checkedGoodsList = iCartService.list(new QueryWrapper<Cart>().eq("checked", true).eq("user_id", userId));
        } else {
            checkedGoodsList = iCartService.listByIds(cartIdArr);
        }

        if (checkedGoodsList.isEmpty()) {
            redisCache.setCacheObject(ORDER_RESULT_KEY.getKey(orderSn), "收获地址为空",
                    ORDER_RESULT_KEY.getExpireSecond());
            throw new BusinessException(ReturnCodeEnum.ORDER_ERROR_CART_EMPTY_ERROR);
        }

        // 商品货品库存数量减少
        List<Long> goodsIds = checkedGoodsList.stream().map(Cart::getGoodsId).collect(Collectors.toList());
        List<GoodsProduct> goodsProducts = iGoodsProductService.list(new QueryWrapper<GoodsProduct>().in("goods_id", goodsIds));
        Map<Long, GoodsProduct> goodsIdMap = goodsProducts.stream().collect(
                Collectors.toMap(GoodsProduct::getId, goodsProduct -> goodsProduct));
        for (Cart checkGoods : checkedGoodsList) {
            Long productId = checkGoods.getProductId();
            Long goodsId = checkGoods.getGoodsId();
            GoodsProduct product = goodsIdMap.get(productId);
            if (product != null) {
                int remainNumber = product.getNumber() - checkGoods.getNumber();
                if (remainNumber < 0) {
                    Goods goods = iGoodsService.getById(goodsId);
                    String goodsName = goods.getName();
                    String[] specifications = product.getSpecifications();
                    throw new BusinessException(String.format(ReturnCodeEnum.ORDER_ERROR_STOCK_NOT_ENOUGH.getMsg(),
                            goodsName, StringUtils.join(specifications, " ")));
                }
                if (!iGoodsProductService.reduceStock(productId, checkGoods.getNumber())) {
                    throw new BusinessException(ReturnCodeEnum.ORDER_SUBMIT_ERROR);
                }
            }
        }

        // 商品费用
        BigDecimal checkedGoodsPrice = new BigDecimal("0.00");
        for (Cart checkGoods : checkedGoodsList) {
            checkedGoodsPrice = checkedGoodsPrice.add(checkGoods.getPrice().multiply(new BigDecimal(checkGoods.getNumber())));
        }

        // 根据订单商品总价计算运费，满足条件（例如88元）则免运费，否则需要支付运费（例如8元）；
        BigDecimal freightPrice = new BigDecimal("0.00");
        if (checkedGoodsPrice.compareTo(WaynConfig.getFreightLimit()) < 0) {
            freightPrice = WaynConfig.getFreightPrice();
        }

        // 订单费用
        BigDecimal orderTotalPrice = checkedGoodsPrice.add(freightPrice).max(BigDecimal.ZERO);

        // 优惠卷抵扣费用
        BigDecimal couponPrice = BigDecimal.ZERO;
        if (userCouponId != null) {
            ShopMemberCoupon memberCoupon = shopMemberCouponService.getById(userCouponId);
            couponPrice = BigDecimal.valueOf(memberCoupon.getDiscount());
        }

        // 最终支付费用
        BigDecimal actualPrice = orderTotalPrice.subtract(couponPrice).max(BigDecimal.ZERO);

        // 组装订单数据
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderSn(orderDTO.getOrderSn());
        order.setOrderStatus(OrderStatusEnum.STATUS_CREATE.getStatus());
        order.setConsignee(checkedAddress.getName());
        order.setMobile(checkedAddress.getTel());
        order.setMessage(orderDTO.getMessage());
        String detailedAddress = checkedAddress.getProvince() + checkedAddress.getCity() + checkedAddress.getCounty() + " " + checkedAddress.getAddressDetail();
        order.setAddress(detailedAddress);
        order.setFreightPrice(freightPrice);
        order.setCouponPrice(couponPrice);
        order.setGoodsPrice(checkedGoodsPrice);
        order.setOrderPrice(orderTotalPrice);
        order.setActualPrice(actualPrice);
        order.setCreateTime(new Date());
        if (!save(order)) {
            throw new BusinessException(ReturnCodeEnum.ORDER_SUBMIT_ERROR);
        }

        Long orderId = order.getId();
        List<OrderGoods> orderGoodsList = new ArrayList<>(checkedGoodsList.size());
        // 添加订单商品表项
        for (Cart cartGoods : checkedGoodsList) {
            // 订单商品
            OrderGoods orderGoods = new OrderGoods();
            orderGoods.setOrderId(orderId);
            orderGoods.setGoodsId(cartGoods.getGoodsId());
            orderGoods.setGoodsSn(cartGoods.getGoodsSn());
            orderGoods.setProductId(cartGoods.getProductId());
            orderGoods.setGoodsName(cartGoods.getGoodsName());
            orderGoods.setPicUrl(cartGoods.getPicUrl());
            orderGoods.setPrice(cartGoods.getPrice());
            orderGoods.setNumber(cartGoods.getNumber());
            orderGoods.setSpecifications(cartGoods.getSpecifications());
            orderGoods.setCreateTime(LocalDateTime.now());
            orderGoodsList.add(orderGoods);
        }
        if (!iOrderGoodsService.saveBatch(orderGoodsList)) {
            throw new BusinessException(ReturnCodeEnum.ORDER_SUBMIT_ERROR);
        }

        // 删除购物车里面的商品信息
        if (CollectionUtils.isEmpty(cartIdArr)) {
            iCartService.remove(new QueryWrapper<Cart>().eq("user_id", userId));
        } else {
            iCartService.removeByIds(cartIdArr);
        }
        // 修改优惠卷使用状态
        if (userCouponId != null) {
            shopMemberCouponService.lambdaUpdate()
                    .set(ShopMemberCoupon::getUseStatus, 1)
                    .set(ShopMemberCoupon::getOrderId, orderId)
                    .eq(ShopMemberCoupon::getId, userCouponId)
                    .eq(ShopMemberCoupon::getUseStatus, 0)
                    .update();
        }

        // 下单30分钟内未支付自动取消订单
        Map<String, Object> map = new HashMap<>();
        map.put("orderSn", orderDTO.getOrderSn());
        map.put("notifyUrl", WaynConfig.getMobileUrl() + "/callback/order/unpaid");
        Message message = MessageBuilder
                .withBody(JSON.toJSONString(map).getBytes(Constants.UTF_ENCODING))
                .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        delayRabbitTemplate.convertAndSend(MQConstants.ORDER_DELAY_EXCHANGE, MQConstants.ORDER_DELAY_ROUTING, message, messagePostProcessor -> {
            // 默认延迟30分钟
            long delayTime = WaynConfig.getUnpaidOrderCancelDelayTime() * DateUnit.MINUTE.getMillis();
            messagePostProcessor.getMessageProperties().setDelay(Math.toIntExact(delayTime));
            return messagePostProcessor;
        });
    }

    @Override
    public String searchResult(String orderSn) {
        String value = redisCache.getCacheObject(ORDER_RESULT_KEY.getKey(orderSn));
        if (value == null) {
            return ORDER_SUBMIT_ERROR_MSG;
        }
        return value;
    }


    @Override
    public void refund(Long orderId) {
        Order order = this.getById(orderId);
        ReturnCodeEnum returnCodeEnum = this.checkOrderOperator(order);
        if (!ReturnCodeEnum.SUCCESS.equals(returnCodeEnum)) {
            throw new BusinessException(returnCodeEnum);
        }

        OrderHandleOption handleOption = OrderUtil.build(order);
        if (!handleOption.isRefund()) {
            throw new BusinessException(ReturnCodeEnum.ORDER_CANNOT_REFUND_ERROR);
        }

        // 设置订单申请退款状态
        if (!this.lambdaUpdate()
                .set(Order::getOrderStatus, OrderStatusEnum.STATUS_REFUND.getStatus())
                .set(Order::getUpdateTime, new Date())
                .set(Order::getRefundStatus, 1)
                .eq(Order::getId, orderId)
                .update()) {
            throw new BusinessException(ReturnCodeEnum.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId) {
        Order order = getById(orderId);
        orderUnpaidService.unpaid(order.getOrderSn(), OrderStatusEnum.STATUS_CANCEL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long orderId) {
        Order order = getById(orderId);
        ReturnCodeEnum returnCodeEnum = checkOrderOperator(order);
        if (!ReturnCodeEnum.SUCCESS.equals(returnCodeEnum)) {
            throw new BusinessException(returnCodeEnum);
        }
        // 检测是否能够取消
        OrderHandleOption handleOption = OrderUtil.build(order);
        if (!handleOption.isDelete()) {
            throw new BusinessException(ReturnCodeEnum.ORDER_CANNOT_DELETE_ERROR);
        }
        // 删除订单
        removeById(orderId);
        // 删除订单商品
        iOrderGoodsService.remove(new QueryWrapper<OrderGoods>().eq("order_id", orderId));
    }

    @Override
    public void confirm(Long orderId) {
        Order order = getById(orderId);
        ReturnCodeEnum returnCodeEnum = checkOrderOperator(order);
        if (!ReturnCodeEnum.SUCCESS.equals(returnCodeEnum)) {
            throw new BusinessException(returnCodeEnum);
        }
        // 检测是否能够取消
        OrderHandleOption handleOption = OrderUtil.build(order);
        if (!handleOption.isConfirm()) {
            throw new BusinessException(ReturnCodeEnum.ORDER_CANNOT_CONFIRM_ERROR);
        }
        // 更改订单状态为已收货
        order.setOrderStatus(OrderStatusEnum.STATUS_CONFIRM.getStatus());
        order.setConfirmTime(LocalDateTime.now());
        order.setUpdateTime(new Date());
        updateById(order);
    }

    @Override
    public ReturnCodeEnum checkOrderOperator(Order order) {
        if (Objects.isNull(order)) {
            return ReturnCodeEnum.USER_NOT_EXISTS_ERROR;
        }
        return ReturnCodeEnum.SUCCESS;
    }

}
