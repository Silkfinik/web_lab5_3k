package org.example.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.entity.Role;
import org.example.entity.User;

import java.io.IOException;
import java.util.Set;

public class SecurityFilter implements Filter {

    private static final Set<String> GUEST_COMMANDS = Set.of(
            "home", "login", "showLoginForm", "register", "showRegisterForm", "logout"
    );

    private static final Set<String> USER_COMMANDS = Set.of(
            "showAllServices",
            "showAllSubscribers",
            "details"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession();

        String command = req.getParameter("command");
        if (command == null) command = "home";

        User user = (User) session.getAttribute("user");
        Role role = (user != null) ? user.getRole() : Role.GUEST;

        if (isAccessAllowed(role, command)) {
            chain.doFilter(request, response);
        } else {
            String errorMsg = (role == Role.GUEST)
                    ? "Пожалуйста, войдите в систему."
                    : "У вас нет прав для выполнения этой операции.";

            session.setAttribute("flashErrorMessage", errorMsg);

            if (role == Role.GUEST) {
                resp.sendRedirect("app?command=showLoginForm");
            } else {
                resp.sendRedirect("app?command=home");
            }
        }
    }

    private boolean isAccessAllowed(Role role, String command) {
        if (GUEST_COMMANDS.contains(command)) return true;
        if (role == Role.ADMIN) return true;
        if (role == Role.USER) return USER_COMMANDS.contains(command);
        return false;
    }
}