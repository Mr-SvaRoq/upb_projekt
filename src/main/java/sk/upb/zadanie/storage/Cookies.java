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

    public String setCookie(HttpServletResponse response, String username) {
        // create a cookie
        Cookie cookie = new Cookie("userName", username);
        //cookie.setMaxAge(7 * 24 * 60 * 60); // expires in 7 days ...ak bude potreba dlhsie trvajuce cookies staci odkomentovat
        cookie.setPath("/");                  // global cookie accessible every where
        cookie.setHttpOnly(true);             // HttpOnly cookies are used to prevent cross-site scripting (XSS) attacks ... ak nepojde java script tak zakomentovat

        //ak sa odkomentuje riadok nizsie tak sa cookies budu posielat len cez https
        //cookie.setSecure(true);

        //add cookie to response
        response.addCookie(cookie);
        return "UserID was written into cookies!";
    }

    //TODO zavolat tuto funkciu pri logoute
    //ale inak podla mojho nazoru sa pri logine prepise userId v cookies takze maybe nepotrebna funkcia ale je peknu ju mat keby nahodou
    //deleteCookie vymaze cookie s nazvom userId
    public String deleteCookie(HttpServletResponse response){

        Cookie cookie = new Cookie("userName", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);

        cookie.setPath("/");

        response.addCookie(cookie);
        return "Cookie deleted!";
    }
}
