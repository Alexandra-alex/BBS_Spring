package com.easybbs.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import com.easybbs.entity.config.WebConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.po.UserInfo;
import com.easybbs.entity.query.UserInfoQuery;
import com.easybbs.exception.BusinessException;
import com.easybbs.mappers.UserInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.easybbs.entity.enums.PageSize;
import com.easybbs.entity.query.EmailCodeQuery;
import com.easybbs.entity.po.EmailCode;
import com.easybbs.entity.vo.PaginationResultVO;
import com.easybbs.entity.query.SimplePage;
import com.easybbs.mappers.EmailCodeMapper;
import com.easybbs.service.EmailCodeService;
import com.easybbs.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 邮箱验证码 业务接口实现
 */
@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService {

	private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery>  userInfoMapper;

	@Resource
	private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;

	@Resource
	private JavaMailSender javaMailSender;

	@Resource
	private WebConfig webConfig;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<EmailCode> findListByParam(EmailCodeQuery param) {
		return this.emailCodeMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(EmailCodeQuery param) {
		return this.emailCodeMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<EmailCode> list = this.findListByParam(param);
		PaginationResultVO<EmailCode> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(EmailCode bean) {
		return this.emailCodeMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<EmailCode> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.emailCodeMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<EmailCode> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.emailCodeMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(EmailCode bean, EmailCodeQuery param) {
		StringTools.checkParam(param);
		return this.emailCodeMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(EmailCodeQuery param) {
		StringTools.checkParam(param);
		return this.emailCodeMapper.deleteByParam(param);
	}

	/**
	 * 根据EmailAndCode获取对象
	 */
	@Override
	public EmailCode getEmailCodeByEmailAndCode(String email, String code) {
		return this.emailCodeMapper.selectByEmailAndCode(email, code);
	}

	/**
	 * 根据EmailAndCode修改
	 */
	@Override
	public Integer updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code) {
		return this.emailCodeMapper.updateByEmailAndCode(bean, email, code);
	}

	/**
	 * 根据EmailAndCode删除
	 */
	@Override
	public Integer deleteEmailCodeByEmailAndCode(String email, String code) {
		return this.emailCodeMapper.deleteByEmailAndCode(email, code);
	}

	//发送验证码
	@Override
	@Transactional(rollbackFor = Exception.class)//如下面的事务执行结果不一致则回滚（比如一个成功一个失败）
	public void  sendEmailCode(String email,Integer type){
		if(type.equals(Constants.ZERO)){
			UserInfo userInfo = userInfoMapper.selectByEmail(email);
			if(userInfo != null){
				throw new BusinessException("邮箱已存在");
			}
		}
		String code = StringTools.getRandomString(Constants.LENGTH_5);

//		sendEmailCodeDo(email,code);//发送验证码

		emailCodeMapper.disableEmailCode(email); //验证验证码是否已使用

		EmailCode emailCode = new EmailCode();
		emailCode.setCode(code);
		emailCode.setEmail(email);
		emailCode.setStatus(Constants.ZERO);
		emailCode.setCreateTime(new Date());
		emailCodeMapper.insert(emailCode);
	}

	//发送邮箱验证码实现
	private void sendEmailCodeDo(String toEmail, String code){
		MimeMessage message = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message,true);
			//邮件发送人
			helper.setFrom(webConfig.getSendUserName());
			//邮件收件人
			helper.setTo(toEmail);
			helper.setSubject("注册邮箱验证码");
			helper.setText("邮箱验证码为"+code);
			helper.setSentDate(new Date());
			javaMailSender.send(message);
		} catch (Exception e) {
			logger.error("发送邮件失败",e);
			throw new RuntimeException("发送邮件失败");
		}
	}

	//检验邮箱和邮箱验证码
	@Override
	public void checkCode(String email, String emailCode) {
		EmailCode dbInfo = this.emailCodeMapper.selectByEmailAndCode(email, emailCode);
		if(dbInfo == null) {
			throw  new BusinessException("邮箱验证码不正确");
		}
		if(!Objects.equals(dbInfo.getStatus(), Constants.ZERO) || System.currentTimeMillis() - dbInfo.getCreateTime().getTime() > 1000L * 60 * Constants.LENGTH_15){
			throw  new BusinessException("邮箱验证码已失效");
		}
		emailCodeMapper.disableEmailCode(email);
	}
}