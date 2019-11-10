package sk.upb.zadanie.storage;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class Cookies {

    public Cookies() {}


    // readCookie precita cookie s nazovm userName
    public String readCookie(@CookieValue(value = "userName", defaultValue = "User") String username) {
        return "Hey! My username is " + username;
    }

    // readCookies by mala precitat vsetky cookies ak nie su zadane ziadne cookies vypise No cookies!
    public String readAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .map(c -> c.getName() + "=" + c.getValue()).collect(Collectors.joining(", "));
        }
        return "No cookies!";
    }

    // setCookie nastavi cookie do session
    // bohuzial oba parametre musia byt typu String
    // z tohto dovodu ukladame do cookies userName
    // nemali by byt dvaja ludia s rovnakym userName tak si myslim ze je to v pohode
    // TODO zavolat tuto funkciu pri logine pouzivatela

    public String setCookieUserNamePassword(HttpServletResponse response, String username, String password) {
        // create a cookie
        Cookie cookieUserName = new Cookie("userName", username);
        //cookie.setMaxAge(7 * 24 * 60 * 60); // expires in 7 days ...ak bude potreba dlhsie trvajuce cookies staci odkomentovat
        cookieUserName.setPath("/");                  // global cookie accessible every where
        cookieUserName.setHttpOnly(true);             // HttpOnly cookies are used to prevent cross-site scripting (XSS) attacks ... ak nepojde java script tak zakomentovat

        Cookie cookiePassword = new Cookie("userPassword", password);
        cookiePassword.setPath("/");
        cookiePassword.setHttpOnly(true);

        //ak sa odkomentuje riadok nizsie tak sa cookies budu posielat len cez https
        //cookie.setSecure(true);

        //add cookie to response
        response.addCookie(cookieUserName);
        response.addCookie(cookiePassword);
        return "UserID was written into cookies!";
    }

    public String getCookieValue(HttpServletRequest request, String cookieName) {
     return Arrays.stream(request.getCookies())
             .filter(cookie -> cookie.getName().equals(cookieName))
             .findFirst()
             .map(Cookie::getValue)
             .orElse(null);
    }


    //TODO zavolat tuto funkciu pri logoute
    //ale inak podla mojho nazoru sa pri logine prepise userId v cookies takze maybe nepotrebna funkcia ale je peknu ju mat keby nahodou
    //deleteCookie vymaze cookie s nazvom userId
    public String deleteCookie(HttpServletResponse response){

        Cookie cookieUserName = new Cookie("userName", null);
        cookieUserName.setMaxAge(0);
        cookieUserName.setHttpOnly(true);
        cookieUserName.setPath("/");


        Cookie cookiePassword = new Cookie("password", null);
        cookiePassword.setMaxAge(0);
        cookiePassword.setHttpOnly(true);
        cookiePassword.setPath("/");


        response.addCookie(cookieUserName);
        response.addCookie(cookiePassword);
        return "Cookie deleted!";
    }
}
