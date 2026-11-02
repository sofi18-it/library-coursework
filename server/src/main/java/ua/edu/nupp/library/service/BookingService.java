package ua.edu.nupp.library.service;

import ua.edu.nupp.library.dao.BookingDao;
import ua.edu.nupp.library.dao.PublicationDao;
import ua.edu.nupp.library.dao.ReaderDao;
import ua.edu.nupp.library.model.Booking;
import ua.edu.nupp.library.model.BookingState;
import ua.edu.nupp.library.model.Publication;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class BookingService {
    private static final Logger LOG=Logger.getLogger(BookingService.class.getName());
    private final BookingDao bookings;private final PublicationDao publications;private final ReaderDao readers;
    public BookingService(BookingDao bookings,PublicationDao publications,ReaderDao readers){this.bookings=bookings;this.publications=publications;this.readers=readers;}
    public List<Booking> list()throws SQLException{return bookings.findAll();}
    public Booking get(long id)throws SQLException{Booking b=bookings.findById(id);if(b==null)throw new ApiException(404,"Бронювання не знайдено.");return b;}
    public Booking create(Booking request)throws SQLException{if(request==null||request.publicationId()<=0||request.readerId()<=0)throw new ApiException(400,"Потрібно вказати видання та читача.");Publication p=publications.findById(request.publicationId());if(p==null)throw new ApiException(400,"Вказане видання не існує.");if(readers.findById(request.readerId())==null)throw new ApiException(400,"Вказаний читач не існує.");LocalDateTime now=LocalDateTime.now();long active=bookings.countActiveForPublication(p.id())+1;IntegralScore s=score(now,active,p);Booking draft=new Booking(null,p.id(),request.readerId(),now,BookingState.CREATED,s.f1(),s.f2(),s.f3(),s.integral());long id=bookings.create(draft);LOG.info("Створено бронювання id="+id);return get(id);}
    public Booking update(long id,Booking request)throws SQLException{Booking current=get(id);if(current.state()!=BookingState.CREATED)throw new ApiException(409,"Редагувати можна лише бронювання у стані CREATED.");if(request==null||request.publicationId()<=0||request.readerId()<=0)throw new ApiException(400,"Потрібно вказати видання та читача.");Publication p=publications.findById(request.publicationId());if(p==null)throw new ApiException(400,"Вказане видання не існує.");if(readers.findById(request.readerId())==null)throw new ApiException(400,"Вказаний читач не існує.");if(!bookings.updateDetails(id,p.id(),request.readerId()))throw new ApiException(409,"Не вдалося змінити бронювання.");return get(id);}
    public void delete(long id)throws SQLException{get(id);if(!bookings.delete(id))throw new ApiException(409,"Видалити можна лише бронювання у стані CREATED.");LOG.info("Видалено бронювання id="+id);}
    public Booking transition(long id,BookingState target)throws SQLException{Booking current=get(id);if(target==null||target.ordinal()!=current.state().ordinal()+1)throw new ApiException(409,"Недопустимий перехід стану. Дозволено лише наступний стан життєвого циклу.");Publication p=publications.findById(current.publicationId());if(p==null)throw new ApiException(409,"Пов'язане видання не знайдено.");if(target==BookingState.READY_FOR_ISSUE&&p.availableCopies()<1)throw new ApiException(409,"Немає доступних примірників для підготовки до видачі.");long active=bookings.countActiveForPublication(p.id());IntegralScore s=score(current.createdAt(),active,p);boolean changed;if(target==BookingState.COMPLETED)changed=bookings.issue(id,s.f1(),s.f2(),s.f3(),s.integral(),LocalDate.now().plusDays(14));else changed=bookings.updateStateAndScores(id,target,s.f1(),s.f2(),s.f3(),s.integral());if(!changed)throw new ApiException(409,"Стан бронювання змінився або операція не виконана.");LOG.info("Бронювання id="+id+" переведено у стан "+target);return get(id);}
    private IntegralScore score(LocalDateTime created,long active,Publication p){return IntegralScore.calculate(created,active,p.totalCopies(),p.availableCopies(),LocalDateTime.now());}
}
