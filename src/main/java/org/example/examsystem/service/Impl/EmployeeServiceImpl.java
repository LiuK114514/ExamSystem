package org.example.examsystem.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.UserMapper;
import org.example.examsystem.service.IService.EmployeeService;
import org.example.examsystem.utils.JwtUtils;
import org.example.examsystem.utils.MailService;
import org.example.examsystem.utils.VerificationCodeService;
import org.example.examsystem.vo.EmployeeLoginVO;
import org.example.examsystem.vo.RegisterResponseVO;
import org.example.examsystem.vo.SendCodeResponseVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserMapper userMapper;
    private final MailService mailService;
    private final VerificationCodeService verificationCodeService;

    @Override
    public EmployeeLoginVO login(String username, String password) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getEmail, username)
                        .eq(User::getPassword, password)
                        .eq(User::getIsDeleted, 0)
        );
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        claims.put("username", user.getEmail());
        String token = JwtUtils.generateJwt(claims);

        EmployeeLoginVO vo = new EmployeeLoginVO();
        vo.setId(user.getId());
        vo.setName(user.getRealName());
        vo.setUserName(user.getEmail());
        vo.setToken(token);

        return vo;
    }

    @Override
    public RegisterResponseVO register(String email, String password, String realName, Integer role, String verificationCode) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("邮箱或密码不能为空");
        }
        if (!StringUtils.hasText(verificationCode)) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        if (!verificationCodeService.validate(email, verificationCode)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        // 邮箱唯一性校验
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getEmail, email)
                        .eq(User::getIsDeleted, 0)
        );
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        // 如果 realName 为空或 null，使用 email 作为默认值
        if (StringUtils.hasText(realName)) {
            user.setRealName(realName);
            System.out.println("设置 realName: " + realName);
        } else {
            user.setRealName(email);
            System.out.println("realName 为空，使用 email 作为默认值: " + email);
        }
        // 注册时强制设置为用户（role=1），不允许通过注册接口创建管理员
        user.setRole(1);
        int rows = userMapper.insert(user);
        if (rows == 0) {
            throw new RuntimeException("注册失败，请稍后再试");
        }
        
        // 返回注册结果VO
        RegisterResponseVO responseVO = new RegisterResponseVO();
        responseVO.setUserId(user.getId());
        responseVO.setEmail(user.getEmail());
        responseVO.setRealName(user.getRealName());
        responseVO.setRole(user.getRole());
        return responseVO;
    }

    @Override
    public SendCodeResponseVO sendRegisterCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        // 已存在则提示
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getEmail, email)
                        .eq(User::getIsDeleted, 0)
        );
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        String code = verificationCodeService.generate(email);
        mailService.sendSimpleMail(mailServiceFrom(), email, "考试系统注册验证码", "您的验证码为：" + code + "，5分钟内有效。");
        
        // 返回发送验证码结果VO
        SendCodeResponseVO responseVO = new SendCodeResponseVO();
        responseVO.setMessage("验证码已发送到您的邮箱，请查收");
        return responseVO;
    }

    @Override
    public void logout(String token) {
        // 系统未持久化会话状态，客户端丢弃 token 即可；这里尝试解析以便给出更友好的反馈
        if (StringUtils.hasText(token)) {
            try {
                JwtUtils.parseJWT(token);
            } catch (Exception ex) {
                throw new IllegalArgumentException("token无效或已过期");
            }
        }
    }

    @Override
    public void editPassword(Long empId, String oldPassword, String newPassword) {
        if (empId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("旧密码和新密码均不能为空");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        int rows = userMapper.update(
                null,
                Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, empId)
                        .eq(User::getIsDeleted, 0)
                        .eq(User::getPassword, oldPassword)
                        .set(User::getPassword, newPassword)
        );
        if (rows == 0) {
            throw new IllegalArgumentException("旧密码错误或用户不存在");
        }
    }

    /**
     * 获取发件人邮箱
     */
    private String mailServiceFrom() {
        // 使用 spring.mail.username 作为发件人
        // 这里直接读取系统属性或环境变量也可，根据项目需求调整
        return "1358709071@qq.com";
    }
}

