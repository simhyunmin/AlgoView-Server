package AlgoView_Server.global.youtube.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class YoutubeService {

    private static final String CALLBACK_URI = "/oauth2callback";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(YouTubeScopes.YOUTUBE_READONLY);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String SESSION_STATE = "state";
    private static final String SESSION_USER_KEY = "user_id";

    @Value("${google.oauth.client-secret}")
    private Resource clientSecretResource;

    @Value("${application.base-url}")
    private String baseUrl;

    private final NetHttpTransport httpTransport;

    public YoutubeService() throws GeneralSecurityException, IOException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    }

    /**
     * OAuth 인증 Flow를 생성합니다.
     */
    private GoogleAuthorizationCodeFlow getFlow() throws IOException {
        // 클라이언트 시크릿 파일 로드
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(clientSecretResource.getInputStream()));

        // 인증 Flow 생성
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
    }

    /**
     * 인증 URL을 생성합니다.
     */
    public String getAuthorizationUrl(HttpServletRequest request) throws IOException {
        // 세션에 사용자 ID 저장 (실제 애플리케이션에서는 로그인된 사용자 ID 사용)
        String userId = UUID.randomUUID().toString();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_KEY, userId);

        // 상태 토큰 생성 (CSRF 방지용)
        String state = UUID.randomUUID().toString();
        session.setAttribute(SESSION_STATE, state);

        // 인증 URL 생성
        AuthorizationCodeFlow flow = getFlow();
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl()
                .setRedirectUri(baseUrl + CALLBACK_URI)
                .setState(state);

        return authorizationUrl.build();
    }

    /**
     * 콜백 URL에서 인증 코드로 토큰을 교환합니다.
     */
    public Credential exchangeCodeForCredential(HttpServletRequest request, String code, String state) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        // 상태 토큰 검증 (CSRF 방지)
        String storedState = (String) session.getAttribute(SESSION_STATE);
        if (storedState == null || !storedState.equals(state)) {
            return null;
        }

        // 사용자 ID 가져오기
        String userId = (String) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            return null;
        }

        // 인증 코드로 토큰 교환
        AuthorizationCodeFlow flow = getFlow();
        TokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(baseUrl + CALLBACK_URI)
                .execute();

        // 사용자 ID에 토큰 저장 및 Credential 반환
        return flow.createAndStoreCredential(tokenResponse, userId);
    }

    /**
     * 세션에서 사용자 ID를 가져와 해당 사용자의 Credential을 반환합니다.
     */
    public Credential getCredential(HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        // 사용자 ID 가져오기
        String userId = (String) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            return null;
        }

        // 사용자 ID로 저장된 Credential 가져오기
        return getFlow().loadCredential(userId);
    }

    /**
     * YouTube 서비스 객체를 생성합니다.
     */
    public YouTube getYouTubeService(Credential credential) {
        return new YouTube.Builder(
                httpTransport, JSON_FACTORY, credential)
                .setApplicationName("AlgoView-YouTube-Integration")
                .build();
    }

    /**
     * 인증 상태를 확인합니다.
     */
    public boolean isAuthenticated(HttpServletRequest request) throws IOException {
        return getCredential(request) != null;
    }
}
