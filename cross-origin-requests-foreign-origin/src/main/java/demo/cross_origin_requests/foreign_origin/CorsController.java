package demo.cross_origin_requests.foreign_origin;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;

@RestController
@RequestMapping
public class CorsController {

    @GetMapping(value = "/time-without-origin")
    public String getTimeNoOrigin() {
        return LocalTime.now().toString();
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/time-any-origin")
    public String getTimeAnyOrigin() {
        return LocalTime.now().toString();
    }

    @CrossOrigin(origins = "http://localhost:8001")
    @GetMapping(value = "/time-explicit-origin")
    public String getTimeExplicitOrigin() {
        return LocalTime.now().toString();
    }
}
