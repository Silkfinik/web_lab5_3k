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
import org.example.exception.DataAccessException;

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

/**
 * Главный сервлет-диспетчер.
 * Обрабатывает все запросы, управляет сессиями, cookies и рендерингом Thymeleaf.
 *
 */
@WebServlet("/app")
public class MainServlet extends HttpServlet {

    private TemplateEngine templateEngine;
    private JakartaServletWebApplication application;

    // DAO
    private SubscriberDao subscriberDao;
    private ServiceDao serviceDao;
    private InvoiceDao invoiceDao;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Инициализация DAO
        this.subscriberDao = new SubscriberDaoImpl();
        this.serviceDao = new ServiceDaoImpl();
        this.invoiceDao = new InvoiceDaoImpl();

        // Принудительная инициализация JPA (на всякий случай)
        try {
            JpaManager.getEntityManager().close();
        } catch (Exception e) {
            throw new ServletException("Ошибка инициализации JPA Manager", e);
        }

        // --- Инициализация Thymeleaf ---
        this.application = JakartaServletWebApplication.buildApplication(getServletContext());

        // Настройка резолвера (поиск шаблонов)
        final WebApplicationTemplateResolver templateResolver =
                new WebApplicationTemplateResolver(this.application);

        templateResolver.setTemplateMode(TemplateMode.HTML);
        // Путь к нашим шаблонам (которые мы скоро создадим)
        templateResolver.setPrefix("/WEB-INF/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        // Отключаем кэширование для удобства разработки
        templateResolver.setCacheable(false);

        // Создание движка Thymeleaf
        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // (Пока что все POST-запросы просто передаем в GET)
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Установка кодировки ответа
        resp.setContentType("text/html;charset=UTF-8");

        // --- Часть 1, Задание 2: Сессии и Cookies ---

        // 1. Сессия (создаем, если ее нет)
        HttpSession session = req.getSession(true);

        // 2. Cookies (счетчик посещений и дата)
        int visitCount = 0;
        String lastVisit = "Never";

        // Поиск существующих cookies
        Optional<Cookie> visitCookie = findCookie(req, "visitCount");
        Optional<Cookie> lastVisitCookie = findCookie(req, "lastVisit");

        if (visitCookie.isPresent()) {
            try {
                visitCount = Integer.parseInt(visitCookie.get().getValue());
            } catch (NumberFormatException e) {
                visitCount = 0;
            }
        }
        if (lastVisitCookie.isPresent()) {
            lastVisit = lastVisitCookie.get().getValue();
        }

        visitCount++; // Увеличиваем счетчик

        // Обновляем cookies
        String currentVisitTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));

        Cookie newVisitCountCookie = new Cookie("visitCount", String.valueOf(visitCount));
        newVisitCountCookie.setMaxAge(60 * 60 * 24 * 365); // 1 год

        Cookie newLastVisitCookie = new Cookie("lastVisit", currentVisitTime);
        newLastVisitCookie.setMaxAge(60 * 60 * 24 * 365); // 1 год

        resp.addCookie(newVisitCountCookie);
        resp.addCookie(newLastVisitCookie);

        // --- Конец Части 1, Задания 2 ---


        // Контекст Thymeleaf
        final WebContext ctx = new WebContext(
                this.application.buildExchange(req, resp)
        );

        // Передаем данные о сессии и cookies в Thymeleaf
        ctx.setVariable("session", session);
        ctx.setVariable("visitCount", visitCount);
        ctx.setVariable("lastVisit", lastVisit);

        // Получаем команду из index.html
        String command = req.getParameter("command");
        if (command == null) {
            command = "default";
        }

        try {
            // Диспетчер команд
            String templateName; // Имя .html файла для рендеринга

            switch (command) {

                case "initData":
                    // Вызов статического метода DataInitializer
                    DataInitializer.insertInitialData(subscriberDao, serviceDao, invoiceDao);

                    // Передача сообщения об успехе в Thymeleaf
                    ctx.setVariable("message", "База данных успешно очищена и заполнена тестовыми данными.");
                    templateName = "init-success"; // init-success.html
                    break;

                case "showAllSubscribers":
                    // Вызов DAO
                    List<Subscriber> subscribers = subscriberDao.findAll();
                    // Передача данных в Thymeleaf
                    ctx.setVariable("subscribers", subscribers);
                    templateName = "subscribers"; // subscribers.html
                    break;

                case "showAllServices":
                    List<Service> services = serviceDao.findAll();
                    ctx.setVariable("services", services);
                    templateName = "services"; // services.html
                    break;

                case "showUnpaidInvoices":
                    List<Invoice> invoices = invoiceDao.findUnpaid();
                    ctx.setVariable("invoices", invoices);
                    templateName = "unpaid-invoices"; // unpaid-invoices.html
                    break;

                // (Другие команды (add, pay) мы добавим позже)

                default:
                    // Если команда не распознана, показываем главную
                    ctx.setVariable("message", "Неизвестная команда: " + command);
                    templateName = "index"; // index.html
                    // (Мы не создавали index.html в /WEB-INF/templates/,
                    // поэтому это приведет к ошибке, если не обработать)
                    // Лучше перенаправить на статический index.html
                    resp.sendRedirect("index.html");
                    return;
            }

            // Рендеринг Thymeleaf-шаблона
            templateEngine.process(templateName, ctx, resp.getWriter());

        } catch (DataAccessException e) {
            // --- Часть 2: Обработка исключений ---
            // При ошибке (например, сбой БД) перенаправляем на error.html
            //

            // System.err.println("Критическая ошибка доступа к данным: " + e.getMessage());
            e.printStackTrace();

            // Передача сообщения об ошибке
            req.setAttribute("errorMessage", e.getMessage());
            req.setAttribute("errorCause", e.getCause() != null ? e.getCause().getMessage() : "N/A");

            // Используем RequestDispatcher для перенаправления на /error.html
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/error.html");
            dispatcher.forward(req, resp);

        } catch (Exception e) {
            // Обработка других (неожиданных) ошибок
            // System.err.println("Неожиданная ошибка: " + e.getMessage());
            e.printStackTrace();

            req.setAttribute("errorMessage", "Неожиданная ошибка приложения.");
            req.setAttribute("errorCause", e.getMessage());

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/error.html");
            dispatcher.forward(req, resp);
        }
    }

    /**
     * Хелпер для поиска Cookie по имени
     */
    private Optional<Cookie> findCookie(HttpServletRequest req, String cookieName) {
        if (req.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(req.getCookies())
                .filter(c -> c.getName().equals(cookieName))
                .findFirst();
    }
}