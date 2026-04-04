package org.example.examsystem.vo;

import lombok.Data;

/**
 * 员工登录返回数据
 */
@Data
public class EmployeeLoginVO {
    private Long id;
    private String name;
    private String token;
    private String userName;
    private String isAdmin;
}


