package send;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController{

    @RequestMapping(value="/signup")
    public void send(@RequestParam(value="userName", required=true) String userName,
    		@RequestParam(value="email", required=true) String email,
    		@RequestParam(value="passHash", required=true) String passHash,
    		@RequestParam(value="passSalt", required=true) String passSalt) {
    	System.out.println("doing stuff");
   
	
    }
}
