package com.education.auth.controller;

import com.education.auth.dto.ChangePasswordDTO;
import com.education.auth.dto.LoginDTO;
import com.education.auth.dto.RegisterDTO;
import com.education.auth.service.AuthService;
import com.education.auth.vo.LoginVO;
import com.education.common.exception.BusinessException;
import com.education.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO vo = authService.login(loginDTO);
        return Result.success(vo);
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }
    
    /**
     * 临时接口：创建或重置admin用户
     * 注意：此接口仅用于开发环境，生产环境应删除或添加权限验证
     */
    @PostMapping("/create-admin")
    public Result<?> createAdmin(@RequestParam(value = "password", defaultValue = "123456") String password) {
        authService.createAdminUser(password);
        return Result.success("Admin用户创建成功");
    }
    
    /**
     * 临时接口：创建或重置admin用户（GET方法，方便浏览器直接访问）
     * 注意：此接口仅用于开发环境，生产环境应删除或添加权限验证
     */
    @GetMapping("/create-admin")
    public Result<?> createAdminGet(@RequestParam(value = "password", defaultValue = "123456") String password) {
        authService.createAdminUser(password);
        return Result.success("Admin用户创建成功");
    }
    
    /**
     * 修改密码
     * 用户登录后可以修改自己的密码
     */
    @PutMapping("/change-password")
    public Result<?> changePassword(
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        
        // 获取当前用户ID
        Long userId = null;
        if (StringUtils.hasText(userIdHeader)) {
            try {
                userId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                throw new BusinessException(400, "无效的用户ID");
            }
        }
        
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        
        // 验证新密码和确认密码是否一致
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new BusinessException(400, "新密码和确认密码不一致");
        }
        
        authService.changePassword(userId, changePasswordDTO.getOldPassword(), changePasswordDTO.getNewPassword());
        return Result.success("密码修改成功");
    }
}

