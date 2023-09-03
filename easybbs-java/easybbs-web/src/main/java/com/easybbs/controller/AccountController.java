package com.easybbs.controller;

import com.easybbs.annotation.GlobalInterceptor;
import com.easybbs.annotation.VerifyParam;
import com.easybbs.controller.base.ABaseController;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.CreateImageCode;
import com.easybbs.entity.dto.SessionWebUserDto;
import com.easybbs.entity.dto.SysSetting4CommentDto;
import com.easybbs.entity.dto.SysSettingDto;
import com.easybbs.entity.enums.VerifyRegexEnum;
import com.easybbs.entity.vo.ResponseVO;
import com.easybbs.exception.BusinessException;
import com.easybbs.service.EmailCodeService;
import com.easybbs.service.UserInfoService;
import com.easybbs.utils.SysCacheUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AccountController extends ABaseController {
    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService  userInfoService;

    //验证码
    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session,Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130,38,5,10);
        response.setHeader("Pragma","no-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        //登录注册
        if(type == null || type == 0){
            session.setAttribute(Constants.CHECK_CODE_KEY,code);
        }
        //获取邮箱
        else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL,code);
        }
        vCode.write(response.getOutputStream());
    }
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(required = true) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type) {
        try{
            //验证码错误
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))){
                throw new BusinessException("图片验证码错误");
            }
            emailCodeService.sendEmailCode(email,type);
            return getSuccessResponseVO(null);
        }finally {
            //用完验证码后清除
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }
    @RequestMapping("/register")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO register(HttpSession session,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max = 150) String email,
                               @VerifyParam(required = true) String emailCode,
                               @VerifyParam(required = true,max = 20) String nickName,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min = 8,max = 18) String password,
                               @VerifyParam(required = true) String checkCode) {
        try {
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.register(email,emailCode,nickName,password);
            return getSuccessResponseVO(null);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
    //登录
    @RequestMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO login(HttpSession session,
                               HttpServletRequest request,
                               @VerifyParam(required = true) String email,
                               @VerifyParam(required = true) String password,
                               @VerifyParam(required = true) String checkCode) {
        try {
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码错误");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password,getIPAddr(request));
            //记录session
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        }finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    //获取用户信息
    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(HttpSession session){
        return  getSuccessResponseVO(getUserInfoFromSession(session));
    }

    //退出登录
    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session){
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    //获取系统设置
    @RequestMapping("/getSysSetting")
    public ResponseVO getSysSetting(){
        SysSettingDto sysSettingDto = SysCacheUtils.getSysSetting();
        //获取评论设置
        SysSetting4CommentDto commentDto = sysSettingDto.getCommentSetting();
        Map<String,Object> result = new HashMap<>();
        result.put("commentOpen",commentDto.getCommentOpen());
        return getSuccessResponseVO(result);
    }

    //重置密码
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true) String email,
                               @VerifyParam(required = true) String emailCode,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min = 8,max = 18) String password,
                               @VerifyParam(required = true) String checkCode
    ){
        try {
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.resetPassword(email,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            //清除图片验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    public static class ForumBoardController extends ABaseController {

    }
}
