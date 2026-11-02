package ua.edu.nupp.library.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ua.edu.nupp.library.dao.*;
import ua.edu.nupp.library.model.*;
import ua.edu.nupp.library.service.*;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LibraryApiServlet extends HttpServlet {
    private static final Logger LOG=Logger.getLogger(LibraryApiServlet.class.getName());
    private final ObjectMapper json=new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private PublicationService publications;private ReaderService readers;private BookingService bookings;private LoanService loans;

    @Override public void init() throws ServletException {
        PublicationDao publicationDao=new PublicationDao();ReaderDao readerDao=new ReaderDao();BookingDao bookingDao=new BookingDao();LoanDao loanDao=new LoanDao();
        publications=new PublicationService(publicationDao);readers=new ReaderService(readerDao);bookings=new BookingService(bookingDao,publicationDao,readerDao);loans=new LoanService(loanDao);
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws IOException{handle(r,s,"GET");}
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException{handle(r,s,"POST");}
    @Override protected void doPut(HttpServletRequest r,HttpServletResponse s)throws IOException{handle(r,s,"PUT");}
    @Override protected void doDelete(HttpServletRequest r,HttpServletResponse s)throws IOException{handle(r,s,"DELETE");}

    private void handle(HttpServletRequest request,HttpServletResponse response,String method)throws IOException {
        response.setCharacterEncoding("UTF-8");response.setContentType("application/json");response.setHeader("X-Content-Type-Options","nosniff");
        try {
            String raw=request.getPathInfo();String[] parts=raw==null?new String[0]:Arrays.stream(raw.split("/")).filter(x->!x.isBlank()).toArray(String[]::new);
            if(parts.length==0)throw new ApiException(404,"Маршрут не знайдено.");
            String resource=parts[0];String idText=parts.length>=2?parts[1]:null;Long id=null;
            if(idText!=null){try{id=Long.parseLong(idText);if(id<=0)throw new NumberFormatException();}catch(NumberFormatException e){throw new ApiException(400,"Ідентифікатор має бути додатним числом.");}}
            if(parts.length==3&&"bookings".equals(resource)&&"transition".equals(parts[2])&&"POST".equals(method)){TransitionRequest t=read(request,TransitionRequest.class);ok(response,200,bookings.transition(id,parseState(t.state())));return;}
            if(parts.length==3&&"loans".equals(resource)&&"return".equals(parts[2])&&"POST".equals(method)){ok(response,200,loans.returnLoan(id));return;}
            if(parts.length>2)throw new ApiException(404,"Маршрут не знайдено.");
            switch(resource) {
                case "publications" -> publicationRoute(method,id,request,response);
                case "readers" -> readerRoute(method,id,request,response);
                case "bookings" -> bookingRoute(method,id,request,response);
                case "loans" -> loanRoute(method,id,request,response);
                default -> throw new ApiException(404,"Маршрут не знайдено.");
            }
        } catch(ApiException e){sendError(response,e.status(),e.getMessage());}
          catch(IllegalArgumentException e){sendError(response,400,e.getMessage()==null?"Некоректні дані запиту.":e.getMessage());}
          catch(SQLException e){LOG.log(Level.SEVERE,"Помилка доступу до MySQL",e);sendError(response,500,"Помилка виконання операції з базою даних.");}
          catch(Exception e){LOG.log(Level.SEVERE,"Помилка HTTP-обробки",e);sendError(response,500,"Внутрішня помилка сервера.");}
    }

    private void publicationRoute(String m,Long id,HttpServletRequest req,HttpServletResponse res)throws Exception {
        if(id==null&&"GET".equals(m)){ok(res,200,publications.list());return;}
        if(id==null&&"POST".equals(m)){ok(res,201,publications.create(read(req,Publication.class)));return;}
        if(id!=null&&"GET".equals(m)){ok(res,200,publications.get(id));return;}
        if(id!=null&&"PUT".equals(m)){ok(res,200,publications.update(id,read(req,Publication.class)));return;}
        if(id!=null&&"DELETE".equals(m)){publications.delete(id);ok(res,200,Map.of("message","Видання видалено."));return;}
        throw new ApiException(405,"Метод не підтримується для цього маршруту.");
    }
    private void readerRoute(String m,Long id,HttpServletRequest req,HttpServletResponse res)throws Exception {
        if(id==null&&"GET".equals(m)){ok(res,200,readers.list());return;}
        if(id==null&&"POST".equals(m)){ok(res,201,readers.create(read(req,Reader.class)));return;}
        if(id!=null&&"GET".equals(m)){ok(res,200,readers.get(id));return;}
        if(id!=null&&"PUT".equals(m)){ok(res,200,readers.update(id,read(req,Reader.class)));return;}
        if(id!=null&&"DELETE".equals(m)){readers.delete(id);ok(res,200,Map.of("message","Читача видалено."));return;}
        throw new ApiException(405,"Метод не підтримується для цього маршруту.");
    }
    private void bookingRoute(String m,Long id,HttpServletRequest req,HttpServletResponse res)throws Exception {
        if(id==null&&"GET".equals(m)){ok(res,200,bookings.list());return;}
        if(id==null&&"POST".equals(m)){ok(res,201,bookings.create(read(req,Booking.class)));return;}
        if(id!=null&&"GET".equals(m)){ok(res,200,bookings.get(id));return;}
        if(id!=null&&"PUT".equals(m)){ok(res,200,bookings.update(id,read(req,Booking.class)));return;}
        if(id!=null&&"DELETE".equals(m)){bookings.delete(id);ok(res,200,Map.of("message","Бронювання видалено."));return;}
        throw new ApiException(405,"Метод не підтримується для цього маршруту.");
    }
    private void loanRoute(String m,Long id,HttpServletRequest req,HttpServletResponse res)throws Exception {
        if(id==null&&"GET".equals(m)){ok(res,200,loans.list());return;}
        if(id!=null&&"GET".equals(m)){ok(res,200,loans.get(id));return;}
        if(id!=null&&"PUT".equals(m)){ok(res,200,loans.update(id,read(req,Loan.class)));return;}
        if(id!=null&&"DELETE".equals(m)){loans.delete(id);ok(res,200,Map.of("message","Видачу видалено."));return;}
        throw new ApiException(405,"Створення видачі виконується переходом бронювання у COMPLETED.");
    }
    private BookingState parseState(String value){try{return BookingState.valueOf(value);}catch(Exception e){throw new ApiException(400,"Невідомий стан бронювання.");}}
    private <T>T read(HttpServletRequest request,Class<T> type)throws IOException{try{return json.readValue(request.getInputStream(),type);}catch(IOException e){throw new ApiException(400,"Тіло запиту містить некоректний JSON.");}}
    private void ok(HttpServletResponse response,int status,Object value)throws IOException{response.setStatus(status);json.writeValue(response.getOutputStream(),Map.of("data",value));}
    private void sendError(HttpServletResponse response,int status,String message)throws IOException{if(response.isCommitted())return;response.setStatus(status);json.writeValue(response.getOutputStream(),Map.of("error",message==null?"Некоректний запит.":message));}
    public record TransitionRequest(String state) { }
}
