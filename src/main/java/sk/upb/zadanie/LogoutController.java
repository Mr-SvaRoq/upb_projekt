package sk.upb.zadanie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sk.upb.zadanie.storage.Cookies;
import sk.upb.zadanie.storage.FileNotFoundException;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LogoutController {
    private final Cookies cookies;

    @Autowired
    public LogoutController(Cookies cookies) {
        this.cookies = cookies;
    }

    @PostMapping({"/logOut"})
    public String logOut(RedirectAttributes redirectAttributes, HttpServletResponse response) {
        redirectAttributes.addFlashAttribute("logout", "Odhlaseny ");
        cookies.deleteCookie(response);
        return "redirect:/login";
    }

    @ExceptionHandler({FileNotFoundException.class})
    public ResponseEntity<?> handleStorageFileNotFound(FileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}