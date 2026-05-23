package org.example.examsystem;
import org.example.examsystem.entity.User;
import org.example.examsystem.mapper.UserMapper;
import org.example.examsystem.service.IService.EmployeeService;
import org.example.examsystem.service.Impl.EmployeeServiceImpl;
import org.example.examsystem.utils.VerificationCodeService;
import org.example.examsystem.vo.RegisterResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private UserMapper employeeMapper;

    private String testEmail;
    private String testPassword;
    private String testRealName;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testPassword = "123456";
        testRealName = "测试用户";
    }

    // ---------------- TC-101 正常注册 ----------------
    @Test
    void TC_101_normal_register() {


        when(verificationCodeService.validate(testEmail, "123456"))
                .thenReturn(true);

        when(employeeMapper.insert((User) any()))
                .thenReturn(1);

        RegisterResponseVO result = employeeService.register(
                testEmail,
                testPassword,
                testRealName,
                null,
                "123456"
        );

        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        assertEquals(testRealName, result.getRealName());
    }

    // ---------------- TC-102 邮箱非法 ----------------
    @Test
    void TC_102_invalid_email() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        "abc123",
                        testPassword,
                        testRealName,
                        null,
                        "123456"
                )
        );

        assertTrue(ex.getMessage().contains("邮箱格式错误"));
    }

    // ---------------- TC-103 密码过短 ----------------
    @Test
    void TC_103_short_password() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        testEmail,
                        "12345",
                        testRealName,
                        null,
                        "123456"
                )
        );

        assertTrue(ex.getMessage().contains("密码长度不足"));
    }

    // ---------------- TC-104 验证码错误 ----------------
    @Test
    void TC_104_wrong_code() {

        when(verificationCodeService.validate(any(), any()))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        testEmail,
                        testPassword,
                        testRealName,
                        null,
                        "999999"
                )
        );
    }

    // ---------------- TC-105 验证码重复使用 ----------------
    @Test
    void TC_105_code_used() {

        when(verificationCodeService.validate(any(), any()))
                .thenReturn(true)
                .thenReturn(false); // 第二次使用失败

        when(employeeMapper.insert((User) any()))
                .thenReturn(1);

        // 第一次注册成功
        employeeService.register(
                testEmail,
                testPassword,
                testRealName,
                null,
                "123456"
        );

        // 第二次使用同验证码失败
        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        "new@example.com",
                        testPassword,
                        testRealName,
                        null,
                        "123456"
                )
        );
    }
    // ---------------- TC-106 邮箱已存在 ----------------
    @Test
    void TC_106_email_already_exists() {
        when(verificationCodeService.validate(any(), any()))
                .thenReturn(true);
        when(employeeMapper.selectCount(any())).thenReturn(1L);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        testEmail,
                        testPassword,
                        testRealName,
                        null,
                        "123456"
                )
        );

        assertTrue(ex.getMessage().contains("该邮箱已注册"));
    }
    // ---------------- TC-107 真实姓名为空 ----------------
    @Test
    void TC_107_realName_empty() {
        when(verificationCodeService.validate(any(), any()))
                .thenReturn(true);
        when(employeeMapper.insert((User) any())).thenReturn(1);
        RegisterResponseVO result = employeeService.register(
                "test@example.com",
                "123456",
                "",   // realName为空
                null,
                "123456"
        );

        assertEquals("test@example.com", result.getRealName());
    }
    // ---------------- TC-108 验证码为空 ----------------
    @Test
    void TC_108_code_empty() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        testEmail,
                        testPassword,
                        testRealName,
                        null,
                        ""
                )
        );

        assertTrue(ex.getMessage().contains("验证码不能为空"));
    }
    // ----------------TC-109 验证码过期 ----------------
    @Test
    void TC_109_code_expired() {

        when(verificationCodeService.validate(any(), any()))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        testEmail,
                        testPassword,
                        testRealName,
                        null,
                        "123456"
                )
        );
    }
    // ---------------- TC-110 正常注册后重复注册 ----------------
    @Test
    void TC_110_duplicate_register() {

        when(employeeMapper.selectCount(any())).thenReturn(0L);

        when(verificationCodeService.validate(any(), any()))
                .thenReturn(true);

        when(employeeMapper.insert((User) any()))
                .thenReturn(1);

        // 第一次注册成功
        employeeService.register(
                testEmail,
                testPassword,
                testRealName,
                null,
                "123456"
        );

        // 第二次重复注册
        when(employeeMapper.selectCount(any())).thenReturn(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> employeeService.register(
                        testEmail,
                        testPassword,
                        testRealName,
                        null,
                        "123456"
                )
        );
    }
}