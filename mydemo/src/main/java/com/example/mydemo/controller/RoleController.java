package com.example.mydemo.controller;

import com.example.mydemo.model.Role;
import com.example.mydemo.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "http://localhost:8081") // 允许跨域请求
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/search")
    public Role getRoleByName(@RequestParam String roleName) {
        return roleService.getRoleByName(roleName);
    }

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/page")
    public Page<Role> getRolesByPage(@RequestParam(defaultValue = "0") Integer page,
                                     @RequestParam(defaultValue = "4") Integer size) {
        return roleService.getAllRolesPageable(page, size);
    }

    @GetMapping("/{id}")
    public Optional<Role> getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    // 创建角色的端点
    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleService.saveRole(role);
    }

    // 更新角色信息的端点
    @PutMapping("/{id}")
    public Role updateRole(@PathVariable Long id, @RequestBody Role role) {
        return roleService.updateRole(id, role);
    }

    // 删除单个角色的端点
    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
    }

    // 批量删除角色的端点
    @DeleteMapping("/batch-delete")
    public void deleteRoles(@RequestBody List<Long> ids) {
        roleService.deleteRoles(ids);
    }

    // 上传头像并更新角色的端点
    @PostMapping("/upload-avatar/{roleId}")
    public ResponseEntity<Role> uploadAvatar(@PathVariable Long roleId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // 定义文件上传路径
        String uploadDir = new File("src/main/resources/static/uploads/avatars").getAbsolutePath();

        // 创建目录，如果不存在则创建
        File folder = new File(uploadDir);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        // 使用原始文件名保存文件
        String fileName = file.getOriginalFilename();
        File dest = new File(folder.getAbsolutePath() + File.separator + fileName);

        try {
            // 保存文件
            file.transferTo(dest);
            // 获取文件路径
            String avatarPath = "/uploads/avatars/" + fileName;

            // 获取角色并更新头像路径
            Optional<Role> optionalRole = roleService.getRoleById(roleId);
            if (optionalRole.isPresent()) {
                Role role = optionalRole.get();
                role.setAvatarPath(avatarPath);
                Role updatedRole = roleService.saveRole(role);
                return ResponseEntity.ok(updatedRole);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
