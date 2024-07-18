package com.wayn.admin.api.controller.shop;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wayn.common.base.controller.BaseController;
import com.wayn.common.core.entity.shop.Goods;
import com.wayn.common.core.service.shop.IGoodsService;
import com.wayn.common.request.GoodsSaveRelatedReqVO;
import com.wayn.data.elastic.constant.EsConstants;
import com.wayn.data.elastic.manager.ElasticDocument;
import com.wayn.data.elastic.manager.ElasticEntity;
import com.wayn.data.redis.constant.RedisKeyEnum;
import com.wayn.data.redis.manager.RedisCache;
import com.wayn.data.redis.manager.RedisLock;
import com.wayn.util.exception.BusinessException;
import com.wayn.util.util.R;
import com.wayn.util.util.file.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品管理
 *
 * @author wayn
 * @since 2020-07-06
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/shop/goods")
public class GoodsController extends BaseController {

    private IGoodsService iGoodsService;
    private ElasticDocument elasticDocument;
    private RedisCache redisCache;
    private RedisLock redisLock;

    /**
     * 商品列表
     *
     * @param goods
     * @return
     */
    @PreAuthorize("@ss.hasPermi('shop:goods:list')")
    @GetMapping("/list")
    public R<IPage<Goods>> list(Goods goods) {
        Page<Goods> page = getPage();
        return R.success(iGoodsService.listPage(page, goods));
    }

    /**
     * 添加商品
     *
     * @param goodsSaveRelatedReqVO
     * @return
     */
    @PreAuthorize("@ss.hasPermi('shop:goods:add')")
    @PostMapping
    public R<Boolean> addGoods(@Validated @RequestBody GoodsSaveRelatedReqVO goodsSaveRelatedReqVO) {
        iGoodsService.saveGoodsRelated(goodsSaveRelatedReqVO);
        return R.success();
    }

    /**
     * 添加商品
     *
     * @param goodsSaveRelatedReqVO
     * @return
     */
    @PreAuthorize("@ss.hasPermi('shop:goods:update')")
    @PutMapping
    public R<Boolean> updateGoods(@Validated @RequestBody GoodsSaveRelatedReqVO goodsSaveRelatedReqVO) throws IOException {
        iGoodsService.updateGoodsRelated(goodsSaveRelatedReqVO);
        return R.success();
    }

    /**
     * 获取商品信息
     *
     * @param goodsId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('shop:goods:info')")
    @GetMapping("{goodsId}")
    public R<Map<String, Object>> getGoods(@PathVariable Long goodsId) {
        return R.success(iGoodsService.getGoodsInfoById(goodsId));
    }

    /**
     * 删除商品
     *
     * @param goodsId
     * @return
     * @throws IOException
     */
    @PreAuthorize("@ss.hasPermi('shop:goods:delete')")
    @DeleteMapping("{goodsId}")
    public R<Boolean> deleteGoods(@PathVariable Long goodsId) throws IOException {
        return R.result(iGoodsService.deleteGoodsRelatedByGoodsId(goodsId));
    }

    /**
     * 商品列表同步es
     *
     * @return
     */
    @PreAuthorize("@ss.hasPermi('shop:goods:syncEs')")
    @PostMapping("syncEs")
    public R<Boolean> syncEs() {
        boolean flag = false;
        try {
            boolean lock = redisLock.lock(RedisKeyEnum.ES_SYNC_CACHE.getKey(), 2);
            if (!lock) {
                throw new BusinessException("加锁失败");
            }
            elasticDocument.deleteIndex(EsConstants.ES_GOODS_INDEX);
            InputStream inputStream = this.getClass().getResourceAsStream(EsConstants.ES_INDEX_GOODS_FILENAME);
            if (elasticDocument.createIndex(EsConstants.ES_GOODS_INDEX, FileUtils.getContent(inputStream))) {
                List<Goods> list = iGoodsService.list();
                List<ElasticEntity> entities = new ArrayList<>();
                for (Goods goods : list) {
                    ElasticEntity elasticEntity = new ElasticEntity();
                    Map<String, Object> map = new HashMap<>();
                    elasticEntity.setId(goods.getId().toString());
                    map.put("id", goods.getId());
                    map.put("name", goods.getName());
                    map.put("pyname", goods.getName());
                    map.put("sales", goods.getVirtualSales());
                    map.put("isHot", goods.getIsHot());
                    map.put("isNew", goods.getIsNew());
                    map.put("countPrice", goods.getCounterPrice());
                    map.put("retailPrice", goods.getRetailPrice());
                    map.put("keyword", goods.getKeywords().split(","));
                    map.put("isOnSale", goods.getIsOnSale());
                    map.put("createTime", goods.getCreateTime());
                    elasticEntity.setData(map);
                    entities.add(elasticEntity);
                }
                flag = elasticDocument.insertBatch(EsConstants.ES_GOODS_INDEX, entities);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            redisLock.unLock(RedisKeyEnum.ES_SYNC_CACHE.getKey());
        }
        return R.result(flag);
    }

}
