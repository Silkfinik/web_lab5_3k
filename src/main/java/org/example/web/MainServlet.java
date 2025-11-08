package org.example.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.example.dao.api.InvoiceDao;
import org.example.dao.api.ServiceDao;
import org.example.dao.api.SubscriberDao;
import org.example.dao.impl.InvoiceDaoImpl;
import org.example.dao.impl.ServiceDaoImpl;
import org.example.dao.impl.SubscriberDaoImpl;
import org.example.db.DataInitializer;
import org.example.db.JpaManager;
import org.example.entity.Invoice;
import org.example.entity.Service;
import org.example.entity.Subscriber;
import org.example.exception.DuplicateEntryException;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@WebServlet("/app")
public class MainServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private JakartaServletWebApplication application;

    private SubscriberDao subscriberDao;
    private ServiceDao serviceDao;
    private InvoiceDao invoiceDao;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.subscriberDao = new SubscriberDaoImpl();
        this.serviceDao = new ServiceDaoImpl();
        this.invoiceDao = new InvoiceDaoImpl();

        try {
            JpaManager.getEntityManager().close();
        } catch (Exception e) {
            throw new ServletException("Ошибка инициализации JPA Manager", e);
        }

        this.application = JakartaServletWebApplication.buildApplication(getServletContext());

        final WebApplicationTemplateResolver templateResolver =
                new WebApplicationTemplateResolver(this.application);

        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");

        HttpSession session = req.getSession(true);
        int visitCount = 0;
        String lastVisit = "Never";

        Optional<Cookie> visitCookie = findCookie(req, "visitCount");
        Optional<Cookie> lastVisitCookie = findCookie(req, "lastVisit");

        if (visitCookie.isPresent()) {
            visitCount = parseIntSafe(visitCookie.get().getValue(), 0);
        }
        if (lastVisitCookie.isPresent()) {
            lastVisit = lastVisitCookie.get().getValue();
        }
        visitCount++;

        String currentVisitTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));

        resp.addCookie(createCookie("visitCount", String.valueOf(visitCount), 3600*24*365));
        resp.addCookie(createCookie("lastVisit", currentVisitTime, 3600*24*365));

        final WebContext ctx = new WebContext(
                this.application.buildExchange(req, resp)
        );
        ctx.setVariable("session", session);
        ctx.setVariable("visitCount", visitCount);
        ctx.setVariable("lastVisit", lastVisit);

        String command = req.getParameter("command");
        if (command == null) {
            command = "default";
        }

        try {
            String templateName = null;

            switch (command) {
                case "showAllSubscribers":
                    List<Subscriber> subscribers = subscriberDao.findAll();
                    ctx.setVariable("subscribers", subscribers);
                    templateName = "subscribers";
                    break;

                case "showAllServices":
                    List<Service> services = serviceDao.findAll();
                    ctx.setVariable("services", services);
                    templateName = "services";
                    break;

                case "showUnpaidInvoices":
                    List<Invoice> invoices = invoiceDao.findUnpaid();
                    ctx.setVariable("invoices", invoices);
                    templateName = "unpaid-invoices";
                    break;

                case "showAddSubscriberForm":
                    templateName = "add-subscriber";
                    break;

                case "block":
                    int blockId = parseIntSafe(req.getParameter("id"), -1);
                    if (blockId > 0) {
                        subscriberDao.block(blockId);
                    }
                    resp.sendRedirect("app?command=showAllSubscribers");
                    return;

                case "pay":
                    int payId = parseIntSafe(req.getParameter("id"), -1);
                    if (payId > 0) {
                        invoiceDao.pay(payId);
                    }
                    resp.sendRedirect("app?command=showUnpaidInvoices");
                    return;

                case "details":
                    int detailId = parseIntSafe(req.getParameter("id"), -1);
                    Subscriber sub = subscriberDao.findById(detailId);
                    if (sub == null) {
                        throw new Exception("Абонент с ID " + detailId + " не найден.");
                    }
                    List<Service> subServices = serviceDao.findBySubscriberId(detailId);
                    List<Invoice> subInvoices = invoiceDao.findBySubscriberId(detailId);

                    List<Service> allServices = serviceDao.findAll();

                    ctx.setVariable("subscriber", sub);
                    ctx.setVariable("services", subServices);
                    ctx.setVariable("invoices", subInvoices);

                    ctx.setVariable("allServices", allServices);

                    templateName = "subscriber-details";
                    break;

                case "initData":
                    DataInitializer.insertInitialData(subscriberDao, serviceDao, invoiceDao);
                    ctx.setVariable("message", "База данных успешно очищена и заполнена тестовыми данными.");
                    templateName = "init-success";
                    break;

                default:
                    resp.sendRedirect("index.html");
                    return;
            }

            if (templateName != null) {
                templateEngine.process(templateName, ctx, resp.getWriter());
            }

        } catch (Exception e) {
            handleError(req, resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String command = req.getParameter("command");
        if (command == null) {
            command = "";
        }

        try {
            switch (command) {
                case "addSubscriber":
                    String name = req.getParameter("name");
                    String phone = req.getParameter("phone");

                    if (name == null || name.trim().isEmpty() ||
                            phone == null || phone.trim().isEmpty()) {

                        final WebContext ctx = new WebContext(this.application.buildExchange(req, resp));
                        ctx.setVariable("errorMessage", "Имя и телефон не могут быть пустыми.");
                        templateEngine.process("add-subscriber", ctx, resp.getWriter());
                        return;
                    }

                    Subscriber newSubscriber = new Subscriber(name, phone, 0.0, false);
                    subscriberDao.add(newSubscriber);

                    resp.sendRedirect("app?command=showAllSubscribers");
                    break;

                case "linkService":
                    int subscriberId = parseIntSafe(req.getParameter("subscriberId"), -1);
                    int serviceId = parseIntSafe(req.getParameter("serviceId"), -1);

                    if (subscriberId > 0 && serviceId > 0) {
                        try {
                            serviceDao.linkServiceToSubscriber(subscriberId, serviceId);
                        } catch (DuplicateEntryException e) {
                            System.err.println("Услуга уже подключена.");
                        }
                    }
                    resp.sendRedirect("app?command=details&id=" + subscriberId);
                    return;

                default:
                    resp.sendRedirect("index.html");
                    break;
            }

        } catch (DuplicateEntryException e) {
            final WebContext ctx = new WebContext(this.application.buildExchange(req, resp));
            templateEngine.process("add-subscriber", ctx, resp.getWriter());

        } catch (Exception e) {
            handleError(req, resp, e);
        }
    }

    /**
     * Хелпер для поиска Cookie
     */
    private Optional<Cookie> findCookie(HttpServletRequest req, String cookieName) {
        if (req.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(req.getCookies())
                .filter(c -> c.getName().equals(cookieName))
                .findFirst();
    }

    /**
     * Хелпер для создания Cookie
     */
    private Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(getServletContext().getContextPath() + "/");
        return cookie;
    }

    /**
     * Хелпер для безопасного парсинга ID
     */
    private int parseIntSafe(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Общий обработчик ошибок
     */
    private void handleError(HttpServletRequest req, HttpServletResponse resp, Exception e)
            throws ServletException, IOException {

        e.printStackTrace();

        req.setAttribute("errorMessage", e.getMessage());
        req.setAttribute("errorCause", e.getCause() != null ? e.getCause().getMessage() : "N/A");

        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/error.html");
        dispatcher.forward(req, resp);
    }
}