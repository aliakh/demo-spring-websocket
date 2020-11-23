package demo.cross_origin_requests.foreign_origin;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;

@Controller
public class JsonpController {

    @RequestMapping(value = "/time", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTime(@RequestParam("callback") String callback) {
        String json = String.format("{'time': '%s'}", LocalTime.now().toString());
        String response = String.format("%s(%s)", callback, json);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
