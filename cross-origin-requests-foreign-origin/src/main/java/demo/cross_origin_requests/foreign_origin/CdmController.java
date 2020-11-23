package demo.cross_origin_requests.foreign_origin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;

@RestController
@RequestMapping
public class CdmController {

    @GetMapping(value = "/cdm-server")
    public String getTime() {
        return LocalTime.now().toString();
    }
}
