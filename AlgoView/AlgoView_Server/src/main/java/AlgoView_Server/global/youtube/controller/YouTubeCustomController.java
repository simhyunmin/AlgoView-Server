package AlgoView_Server.global.youtube.controller;

import AlgoView_Server.global.youtube.service.YoutubeService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeCustomController {

    private final YoutubeService youtubeService;

    @Autowired
    public YouTubeCustomController(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    /**
     * 유저를 YouTube OAuth 인증 페이지로 리다이렉트합니다.
     */
    @GetMapping("/auth")
    public RedirectView authorize(HttpServletRequest request) throws IOException {
        String authUrl = youtubeService.getAuthorizationUrl(request);
        return new RedirectView(authUrl);
    }

    /**
     * Google OAuth2 인증 콜백을 처리합니다.
     */
    @GetMapping("/oauth2callback")
    public RedirectView oauth2Callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Credential credential = youtubeService.exchangeCodeForCredential(request, code, state);
        if (credential == null) {
            return new RedirectView("/auth-failed");
        }

        // 인증 성공 후 리다이렉트할 페이지
        return new RedirectView("/auth-success");
    }

    /**
     * 현재 인증 상태를 확인합니다.
     */
    @GetMapping("/auth-status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request) throws IOException {
        boolean isAuthenticated = youtubeService.isAuthenticated(request);

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", isAuthenticated);

        return ResponseEntity.ok(response);
    }

    /**
     * 로그인한 사용자의 구독 목록을 조회합니다.
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<?> getUserSubscriptions(
            @RequestParam(defaultValue = "10") long maxResults,
            HttpServletRequest request) {

        try {
            // 인증 자격 증명 가져오기
            Credential credential = youtubeService.getCredential(request);

            // 자격 증명이 없으면 인증 필요 응답
            if (credential == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Authentication required");
                response.put("authUrl", "/api/youtube/auth");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // YouTube 서비스 생성
            YouTube youtube = youtubeService.getYouTubeService(credential);

            // 구독 목록 조회
            List<String> parts = Arrays.asList("snippet", "contentDetails");
            YouTube.Subscriptions.List list = youtube.subscriptions()
                    .list(parts)
                    .setMine(true)
                    .setMaxResults(maxResults);

            SubscriptionListResponse response = list.execute();
            List<Subscription> subscriptions = response.getItems();
            System.out.println(subscriptions);
            return ResponseEntity.ok(subscriptions);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to fetch subscriptions");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/liked")
    public ResponseEntity getUserLikedVideos(

            @RequestParam(defaultValue = "10") long maxResults,
            HttpServletRequest request) {
        try {
            // 인증 자격 증명 가져오기
            Credential credential = youtubeService.getCredential(request);

            // 자격 증명이 없으면 인증 필요 응답
            if (credential == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Authentication required");
                response.put("authUrl", "/api/youtube/auth");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // YouTube 서비스 생성
            YouTube youtube = youtubeService.getYouTubeService(credential);

            // 좋아요 영상 목록 조회
            YouTube.Videos.List list = youtube.videos()
                    .list(Arrays.asList("snippet", "contentDetails"))
                    .setMyRating("like")
                    .setMaxResults(maxResults);

            VideoListResponse response = list.execute();
            List<Video> likedVideos = response.getItems();
            return ResponseEntity.ok(likedVideos);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to fetch liked videos");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 로그아웃 (토큰 취소)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        try {
            // 세션 무효화
            request.getSession(false).invalidate();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully logged out");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Logout failed: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}