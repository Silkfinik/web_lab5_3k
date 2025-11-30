package org.example.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.dao.api.*;
import org.example.dao.impl.*;
import org.example.db.DataInitializer;
import org.example.db.JpaManager;
import org.example.entity.*;
import org.example.exception.DuplicateEntryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

public class FrontControllerFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(FrontControllerFilter.class);

    private TemplateEngine templateEngine;
    private JakartaServletWebApplication application;

    private SubscriberDao subscriberDao;
    private ServiceDao serviceDao;
    private InvoiceDao invoiceDao;
    private UserDao userDao;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.subscriberDao = new SubscriberDaoImpl();
        this.serviceDao = new ServiceDaoImpl();
        this.invoiceDao = new InvoiceDaoImpl();
        this.userDao = new UserDaoImpl();

        this.application = JakartaServletWebApplication.buildApplication(filterConfig.getServletContext());
        final WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(this.application);
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String command = req.getParameter("command");
        if (command == null) {
            resp.sendRedirect("app?command=home");
            return;
        }

        try {
            if (req.getMethod().equalsIgnoreCase("GET")) {
                processGet(command, req, resp);
            } else if (req.getMethod().equalsIgnoreCase("POST")) {
                processPost(command, req, resp);
            }
        } catch (Exception e) {
            handleError(req, resp, e);
        }
    }

    private void processGet(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        WebContext ctx = buildContext(req, resp);
        String templateName = null;

        switch (command) {
            case "home":
                templateName = "home";
                break;
            case "showLoginForm":
                templateName = "login";
                break;
            case "showRegisterForm":
                templateName = "register";
                break;
            case "logout":
                req.getSession().invalidate();
                resp.sendRedirect("app?command=home");
                return;

            case "showAllSubscribers":
                ctx.setVariable("subscribers", subscriberDao.findAll());
                templateName = "subscribers";
                break;
            case "showAllServices":
                ctx.setVariable("services", serviceDao.findAll());
                templateName = "services";
                break;
            case "showUnpaidInvoices":
                ctx.setVariable("invoices", invoiceDao.findUnpaid());
                templateName = "unpaid-invoices";
                break;
            case "showAddSubscriberForm":
                templateName = "add-subscriber";
                break;
            case "details":
                handleDetails(req, ctx);
                templateName = "subscriber-details";
                break;
            case "block":
                int blockId = parseIntSafe(req.getParameter("id"), -1);
                if (blockId > 0) subscriberDao.block(blockId);
                resp.sendRedirect("app?command=showAllSubscribers");
                return;
            case "pay":
                int payId = parseIntSafe(req.getParameter("id"), -1);
                if (payId > 0) invoiceDao.pay(payId);
                resp.sendRedirect("app?command=showUnpaidInvoices");
                return;
            case "initData":
                DataInitializer.insertInitialData(subscriberDao, serviceDao, invoiceDao, userDao);
                ctx.setVariable("message", "База данных сброшена. Создан Admin (login: admin, pass: admin)");
                templateName = "init-success";
                break;
            default:
                resp.sendRedirect("app?command=home");
                return;
        }

        if (templateName != null) {
            templateEngine.process(templateName, ctx, resp.getWriter());
        }
    }

    private void processPost(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        try {
            switch (command) {
                case "login":
                    handleLogin(req, resp);
                    break;
                case "register":
                    handleRegistration(req, resp);
                    break;
                case "addSubscriber":
                    handleAddSubscriber(req, resp);
                    break;
                case "linkService":
                    handleLinkService(req, resp);
                    break;
                default:
                    resp.sendRedirect("app?command=home");
                    break;
            }
        } catch (DuplicateEntryException e) {
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("errorMessage", e.getMessage());
            String source = req.getParameter("source");
            String errorTemplate = "home";
            if ("register".equals(source)) errorTemplate = "register";
            else if ("addSubscriber".equals(source)) errorTemplate = "add-subscriber";

            templateEngine.process(errorTemplate, ctx, resp.getWriter());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String login = req.getParameter("login");
        String pass = req.getParameter("password");

        User user = userDao.findByLogin(login);

        if (user != null && user.getPassword().equals(pass)) {
            req.getSession().setAttribute("user", user);
            resp.sendRedirect("app?command=home");
        } else {
            WebContext ctx = buildContext(req, resp);
            ctx.setVariable("errorMessage", "Неверный логин или пароль");
            templateEngine.process("login", ctx, resp.getWriter());
        }
    }

    private void handleRegistration(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String login = req.getParameter("login");
        String pass = req.getParameter("password");

        if (login == null || login.isBlank() || pass == null || pass.isBlank()) {
            throw new DuplicateEntryException("Логин и пароль не могут быть пустыми.");
        }

        User user = new User(login, pass, Role.USER);
        // Чит для регистрации админа вручную оставим, но основной способ теперь через initData
        if ("admin".equalsIgnoreCase(login)) {
            user.setRole(Role.ADMIN);
        }

        userDao.add(user);
        req.getSession().setAttribute("user", user);
        resp.sendRedirect("app?command=home");
    }

    private void handleAddSubscriber(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getParameter("name");
        String phone = req.getParameter("phone");
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) throw new DuplicateEntryException("Имя и телефон обязательны.");
        Subscriber sub = new Subscriber(name, phone, 0.0, false);
        subscriberDao.add(sub);
        resp.sendRedirect("app?command=showAllSubscribers");
    }

    private void handleLinkService(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int subId = parseIntSafe(req.getParameter("subscriberId"), -1);
        int srvId = parseIntSafe(req.getParameter("serviceId"), -1);
        if (subId > 0 && srvId > 0) serviceDao.linkServiceToSubscriber(subId, srvId);
        resp.sendRedirect("app?command=details&id=" + subId);
    }

    private void handleDetails(HttpServletRequest req, WebContext ctx) {
        int id = parseIntSafe(req.getParameter("id"), -1);
        Subscriber sub = subscriberDao.findById(id);
        if (sub != null) {
            ctx.setVariable("subscriber", sub);
            ctx.setVariable("services", serviceDao.findBySubscriberId(id));
            ctx.setVariable("invoices", invoiceDao.findBySubscriberId(id));
            ctx.setVariable("allServices", serviceDao.findAll());
        }
    }

    private WebContext buildContext(HttpServletRequest req, HttpServletResponse resp) {
        HttpSession session = req.getSession(true);

        int visitCount = 0;
        String lastVisit = "Never";
        Optional<Cookie> visitCookie = Arrays.stream(req.getCookies() == null ? new Cookie[0] : req.getCookies())
                .filter(c -> c.getName().equals("visitCount")).findFirst();
        if (visitCookie.isPresent()) visitCount = parseIntSafe(visitCookie.get().getValue(), 0);
        visitCount++;
        String currentVisitTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
        Cookie c1 = new Cookie("visitCount", String.valueOf(visitCount)); c1.setPath("/"); c1.setMaxAge(3600*24);
        Cookie c2 = new Cookie("lastVisit", currentVisitTime); c2.setPath("/"); c2.setMaxAge(3600*24);
        resp.addCookie(c1);
        resp.addCookie(c2);

        WebContext ctx = new WebContext(this.application.buildExchange(req, resp));
        ctx.setVariable("visitCount", visitCount);
        ctx.setVariable("lastVisit", lastVisit);
        ctx.setVariable("session", session);
        ctx.setVariable("currentUser", session.getAttribute("user"));

        String flashError = (String) session.getAttribute("flashErrorMessage");
        if (flashError != null) {
            ctx.setVariable("errorMessage", flashError);
            session.removeAttribute("flashErrorMessage");
        }

        return ctx;
    }

    private void handleError(HttpServletRequest req, HttpServletResponse resp, Exception e) throws ServletException, IOException {
        logger.error("Error", e);
        req.setAttribute("errorMessage", e.getMessage());
        req.getRequestDispatcher("/error.html").forward(req, resp);
    }

    private int parseIntSafe(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }

    @Override public void destroy() { JpaManager.getEntityManager().close(); }
}