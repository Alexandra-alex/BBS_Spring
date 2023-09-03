package com.easybbs.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.easybbs.entity.config.WebConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.SessionWebUserDto;
import com.easybbs.entity.enums.*;
import com.easybbs.entity.po.UserIntegralRecord;
import com.easybbs.entity.po.UserMessage;
import com.easybbs.entity.query.UserIntegralRecordQuery;
import com.easybbs.entity.query.UserMessageQuery;
import com.easybbs.exception.BusinessException;
import com.easybbs.mappers.UserIntegralRecordMapper;
import com.easybbs.mappers.UserMessageMapper;
import com.easybbs.service.EmailCodeService;
import com.easybbs.utils.JsonUtils;
import com.easybbs.utils.OKHttpUtils;
import com.easybbs.utils.SysCacheUtils;
import com.mysql.cj.xdevapi.JsonParser;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.easybbs.entity.query.UserInfoQuery;
import com.easybbs.entity.po.UserInfo;
import com.easybbs.entity.vo.PaginationResultVO;
import com.easybbs.entity.query.SimplePage;
import com.easybbs.mappers.UserInfoMapper;
import com.easybbs.service.UserInfoService;
import com.easybbs.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户信息 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {

	private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private EmailCodeService emailCodeService;

	@Resource
	private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;

	@Resource
	private UserIntegralRecordMapper<UserIntegralRecord, UserIntegralRecordQuery> userIntegralRecordMapper;

	@Resource
	private WebConfig webConfig;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	/**
	 * 根据NickName获取对象
	 */
	@Override
	public UserInfo getUserInfoByNickName(String nickName) {
		return this.userInfoMapper.selectByNickName(nickName);
	}

	/**
	 * 根据NickName修改
	 */
	@Override
	public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
		return this.userInfoMapper.updateByNickName(bean, nickName);
	}

	/**
	 * 根据NickName删除
	 */
	@Override
	public Integer deleteUserInfoByNickName(String nickName) {
		return this.userInfoMapper.deleteByNickName(nickName);
	}

	//注册
	@Override
	@Transactional(rollbackFor = Exception.class)//回滚
	public void register(String email, String emailCode, String nickName, String password) {
		//检验账号是否已存在
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if(null != userInfo){
			throw new BusinessException("邮箱账号已存在");
		}
		userInfo =this.userInfoMapper.selectByNickName(nickName);
		if(null != userInfo){
			throw new BusinessException("昵称已存在");
		}
		emailCodeService.checkCode(email,emailCode);
		String userId =StringTools.getRandomNumber(Constants.LENGTH_10);
		UserInfo insertInfo =  new UserInfo();
		insertInfo.setUserId(userId);
		insertInfo.setNickName(nickName);
		insertInfo.setEmail(email);
		insertInfo.setPassword(StringTools.encodeMd5(password));
		insertInfo.setJoinTime(new Date());
		insertInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		insertInfo.setTotalIntegral(Constants.ZERO);
		insertInfo.setCurrentIntegral(Constants.ZERO);
		this.userInfoMapper.insert(insertInfo);

		//更新用户积分
		updateUserIntegral(userId,UserIntegralOperTypeEnum.REGISTER,UserIntegralChangeTypeEnum.ADD.getChangeType(),Constants.INTEGRAL_5);

		//记录消息
		UserMessage userMessage = new UserMessage();
		userMessage.setReceivedUserId(userId);
		userMessage.setMessageType(MessageTypeEnum.SYS.getType());
		userMessage.setCreateTime(new Date());
		userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
		userMessage.setMessageContent(SysCacheUtils.getSysSetting().getRegisterSetting().getRegisterWelcomInfo());
		userMessageMapper.insert(userMessage);
	}
	//更新用户积分
	@Transactional(rollbackFor = Exception.class)
	public void updateUserIntegral(String userId, UserIntegralOperTypeEnum OperTypeEnum, Integer changeType, Integer integral){
		  integral = changeType*integral;
		  if(integral==0){
			return;
		  }
		  UserInfo userInfo = userInfoMapper.selectByUserId(userId);
		  //确保积分不会为负
		  if(UserIntegralChangeTypeEnum.REDUCE.getChangeType().equals(changeType)&&userInfo.getCurrentIntegral()+integral<0){
			integral=changeType*userInfo.getCurrentIntegral();
		  }
		  //积分记录表
		  UserIntegralRecord record = new UserIntegralRecord();
		  record.setUserId((userId));
		  record.setOperType((OperTypeEnum.getOperType()));
		  record.setCreateTime(new Date());
		  record.setIntegral(integral);
		  this.userIntegralRecordMapper.insert(record);

		  Integer count = this.userInfoMapper.updateIntegral(userId,integral);
		  if(count==0){
			  throw new BusinessException("更新用户积分失败");
		  }
	}

	//登录
	@Override
	public SessionWebUserDto login(String email, String password, String ip) {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if(userInfo == null || !userInfo.getPassword().equals(password)) {
			throw new BusinessException("账号或者密码错误");
		}
		if(!UserStatusEnum.ENABLE.getStatus().equals(userInfo.getStatus()))
		{
			throw new BusinessException("账号已封禁");
		}

		//获取地址
		String ipAddress = getIpAddress(ip);
		UserInfo updateInfo = new UserInfo();
		updateInfo.setLastLoginTime(new Date());
		updateInfo.setLastLoginIp(ip);
		updateInfo.setLastLoginIpAddress(ipAddress);
		this.userInfoMapper.updateByUserId(updateInfo,userInfo.getUserId());

		//Session
		SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
		sessionWebUserDto.setNickname(userInfo.getNickName());
		sessionWebUserDto.setProvince(ipAddress);
		sessionWebUserDto.setUserId(userInfo.getUserId());
		//设置超级管理员
        sessionWebUserDto.setAdmin(StringTools.isEmpty(webConfig.getAdminEmails()) && ArrayUtils.contains(webConfig.getAdminEmails().split(","), userInfo.getEmail()));
		return sessionWebUserDto;
	}

	//获取IP地址
	public String getIpAddress(String ip){
		try{
			String url = "https://api.ipdatacloud.com/v2/query?key=82844367459211ee9cb900163e25360e&ip="+ip;
			String responseJson = OKHttpUtils.getRequest(url);
			if(responseJson == null)
			{
				return Constants.NO_ADDRESS;
			}
			JSONObject addressInfo = JSON.parseObject(responseJson);
		    JSONObject address1 = addressInfo.getJSONObject("data");
			JSONObject address2 = address1.getJSONObject("location");
			if(address2.getString("province").equals("保留"))
			{
				return Constants.NO_ADDRESS;
			}
			return address2.getString("province");
		}catch (Exception e){
			logger.error("获取IP地址失败",e);
		}
		return Constants.NO_ADDRESS;
	}

	//重置密码
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void resetPassword(String email, String password, String emailCode) {
		//检验账号是否已存在
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if(null == userInfo){
			throw new BusinessException("邮箱账号不存在");
		}
		emailCodeService.checkCode(email,emailCode);
		UserInfo updateInfo = new UserInfo();
		updateInfo.setPassword(StringTools.encodeMd5(password));
		this.userInfoMapper.updateByEmail(updateInfo,email);
	}
}


