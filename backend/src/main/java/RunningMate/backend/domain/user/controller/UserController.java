package RunningMate.backend.domain.user.controller;

import RunningMate.backend.domain.user.dto.UserDTO;
import RunningMate.backend.domain.user.entity.User;
import RunningMate.backend.domain.user.service.UserService;
import RunningMate.backend.domain.authorization.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final SessionUtils sessionUtils;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "사용자에게 nickname, email, password, weight, height를 받아 회원가입을 진행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "회원가입 실패")
    })
    public ResponseEntity<?> signUp(@RequestBody UserDTO.SignUpRequest request){ // 추후 response type, cookie or session 추가
        try {
            User user = userService.signUp(request);
            return ResponseEntity.ok("회원가입에 성공하였습니다.");
        }catch (Exception e){
            return ResponseEntity.internalServerError().body("회원가입 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자에게 email, password를 받아 로그인을 진행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "로그인 실패")
    })
    public ResponseEntity<?> login(@RequestBody UserDTO.LoginRequest request, HttpSession session) {
        try {
            User user = userService.logIn(request);
            session.setAttribute("userId", user.getUserId());
            log.info("{} 님 로그인, userId = {}", user.getUserNickname(), user.getUserId());
            return ResponseEntity.ok().body(user.getUserNickname() + "님, 로그인 성공하셨습니다!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("로그인 실패: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자에게 요청을 받아 로그아웃을 실행해준다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "로그아웃 실패")
    })
    public ResponseEntity<?> logout(HttpSession session) {
        try {
            Optional<User> optionalUser = sessionUtils.getUserFromSession(session);
            session.invalidate();
            return ResponseEntity.ok().body(optionalUser.get().getUserNickname() + " 님 안녕히 가십쇼!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/profile")
    @Operation(summary = "프로필", description = "사용자의 프로필을 제공한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "400", description = "프로필 조회 실패")
    })
    public ResponseEntity<?> profile(HttpSession session){
        try {
            Optional<User> optionalUser = sessionUtils.getUserFromSession(session);

            return ResponseEntity.ok().body(userService.profile(optionalUser));
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/profile/update")
    @Operation(summary = "프로필 업데이트", description = "사용자에게 체중, 신장을 받아 프로필 업데이트를 진행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "업데이트 실패")
    })
    public ResponseEntity<?> updateProfile(@RequestBody UserDTO.UpdateProfileRequest request, HttpSession session){ // 추후 response type, cookie or session 추가
        try {
            Optional<User> optionalUser = sessionUtils.getUserFromSession(session);
            User user = userService.updateProfile(request, optionalUser);
            return ResponseEntity.ok().body(user.getUserNickname() + user.getUserWeight() + user.getUserHeight());
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/posts")
    @Operation(summary = "사용자가 작성한 글 확인", description = "커뮤니티에 등록한 게시글을 확인한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 완료"),
            @ApiResponse(responseCode = "204", description = "작성한 게시글이 없음"),
            @ApiResponse(responseCode = "403", description = "조회 권한이 없음")
    })
    public ResponseEntity<?> viewMyPosts(HttpSession session) {
        try {
            Optional<User> optionalUser = sessionUtils.getUserFromSession(session);
            List<UserDTO.MyPostResponse> myPosts = userService.viewMyPost(optionalUser);

            if (myPosts.isEmpty()) {
                return ResponseEntity.noContent().build();
            } else{
                return ResponseEntity.ok(myPosts);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/records")
    @Operation(summary = "사용자의 운동 기록 확인", description = "사용자의 운동 기록을 확인한다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 완료"),
            @ApiResponse(responseCode = "204", description = "운동 기록이 없음"),
            @ApiResponse(responseCode = "403", description = "조회 권한이 없음")
    })
    public ResponseEntity<?> viewMyRecords(HttpSession session) {
        try {
            Optional<User> optionalUser = sessionUtils.getUserFromSession(session);
            List<UserDTO.MyRecordResponse> myRecords = userService.viewMyRecord(optionalUser);

            if (myRecords.isEmpty()) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.ok(myRecords);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
