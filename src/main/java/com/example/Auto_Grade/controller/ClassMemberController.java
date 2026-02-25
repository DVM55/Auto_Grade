package com.example.Auto_Grade.controller;

import com.example.Auto_Grade.dto.req.ClassCodeRequest;
import com.example.Auto_Grade.dto.res.ApiResponse;
import com.example.Auto_Grade.dto.res.ClassMemberResponse;
import com.example.Auto_Grade.service.ClassMemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/class/member")
@RequiredArgsConstructor
public class ClassMemberController {

    private final ClassMemberService classMemberService;

    // ================= JOIN CLASS =================
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Void>> joinClass(
            @Valid @RequestBody ClassCodeRequest request
    ) {

        classMemberService.joinClass(request);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Gửi yêu cầu tham gia lớp thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= APPROVE MEMBER =================
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveMember(
            @PathVariable Long id
    ) {

        classMemberService.approveMember(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Phê duyệt thành viên thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= REMOVE MEMBER =================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id
    ) {

        classMemberService.removeMember(id);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Xóa thành viên thành công")
                        .data(null)
                        .build()
        );
    }

    // ================= GET PENDING MEMBERS =================
    @GetMapping("/{classId}/pending")
    public ResponseEntity<ApiResponse<Page<ClassMemberResponse>>> getPendingMembers(
            @PathVariable Long classId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ClassMemberResponse> result =
                classMemberService.getPendingMembers(
                        classId,
                        username,
                        email,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<ClassMemberResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách chờ duyệt thành công")
                        .data(result)
                        .build()
        );
    }

    // ================= GET APPROVED MEMBERS =================
    @GetMapping("/{classId}/approved")
    public ResponseEntity<ApiResponse<Page<ClassMemberResponse>>> getApprovedMembers(
            @PathVariable Long classId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ClassMemberResponse> result =
                classMemberService.getApprovedMembers(
                        classId,
                        username,
                        email,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<ClassMemberResponse>>builder()
                        .code(HttpServletResponse.SC_OK)
                        .message("Lấy danh sách thành viên thành công")
                        .data(result)
                        .build()
        );
    }
}
