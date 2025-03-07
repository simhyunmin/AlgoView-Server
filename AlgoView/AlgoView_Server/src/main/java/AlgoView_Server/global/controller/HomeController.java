package AlgoView_Server.global.controller;

import AlgoView_Server.domain.user.dto.SessionUser;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final HttpSession httpSession;

    @GetMapping("/")
    public String postList(Model model) {

        //HTTP 세션에서 사용자 정보 꺼내기
        //로그인 한 후 세션에 저장된 사용자 정보를 가져와준다.
        //httpSession은 현재 HTTP 세션을 나타내는 객체
        SessionUser user = (SessionUser) httpSession.getAttribute("user");
        if (user != null) {
            model.addAttribute("userName", user.getName());
        }
        return "posts/list";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
