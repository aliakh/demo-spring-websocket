package demo.cross_origin_requests.foreign_origin;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

@Controller
public class XfoController {

    public static final String HTML = "<html><head><title>X-Frame-Options example</title></head><body>Hello!</body></html>";

    @GetMapping(value = "/html-no-xfo", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getHtmlNoXfo() {
        return HTML;
    }

    @GetMapping(value = "/html-xfo-deny", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getHtmlXfoDeny(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "DENY");
        return HTML;
    }

    @GetMapping(value = "/html-xfo-sameorigin", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getHtmlXfoSameOrigin(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        return HTML;
    }

    @GetMapping(value = "/html-xfo-allowfrom", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getHtmlXfoAllowFrom(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "ALLOW-FROM http://localhost:8001");
        return HTML;
    }
}
