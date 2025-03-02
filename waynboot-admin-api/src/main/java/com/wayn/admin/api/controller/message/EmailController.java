package com.wayn.admin.api.controller.message;

import com.wayn.common.core.entity.tool.EmailConfig;
import com.wayn.common.core.service.tool.IMailConfigService;
import com.wayn.common.core.vo.SendMailVO;
import com.wayn.common.util.MailUtil;
import com.wayn.util.util.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 邮件处理器
 * @author wayn
 * @since 2020-07-06
 */
@RestController
@AllArgsConstructor
@RequestMapping("callback/email")
public class EmailController {

    private IMailConfigService mailConfigService;

    /**
     * 发送邮件
     *
     * @param subject 邮件标题
     * @param content 邮件内容
     * @param tos     接收人列表
     * @return R
     */
    @PostMapping
    public R<Boolean> sendEmail(String subject, String content, String tos) {
        EmailConfig emailConfig = mailConfigService.getById(1L);
        SendMailVO sendMailVO = new SendMailVO();
        sendMailVO.setSubject(subject);
        sendMailVO.setContent(content);
        sendMailVO.setTos(Collections.singletonList(tos));
        MailUtil.sendMail(emailConfig, sendMailVO, false);
        return R.success();
    }
}
