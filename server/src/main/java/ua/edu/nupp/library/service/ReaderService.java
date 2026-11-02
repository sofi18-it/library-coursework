package ua.edu.nupp.library.service;

import ua.edu.nupp.library.dao.ReaderDao;
import ua.edu.nupp.library.model.Reader;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class ReaderService {
    private static final Logger LOG=Logger.getLogger(ReaderService.class.getName());
    private final ReaderDao dao;
    public ReaderService(ReaderDao dao){this.dao=dao;}
    public List<Reader> list() throws SQLException{return dao.findAll();}
    public Reader get(long id)throws SQLException{Reader r=dao.findById(id);if(r==null)throw new ApiException(404,"Читача не знайдено.");return r;}
    public Reader create(Reader r)throws SQLException{validate(r);if(dao.findByCard(r.cardNumber())!=null)throw new ApiException(409,"Читацький квиток уже зареєстрований.");long id=dao.create(r);LOG.info("Створено читача id="+id);return get(id);}
    public Reader update(long id,Reader r)throws SQLException{get(id);validate(r);Reader same=dao.findByCard(r.cardNumber());if(same!=null&&!same.id().equals(id))throw new ApiException(409,"Читацький квиток уже зареєстрований.");if(!dao.update(id,r))throw new ApiException(409,"Не вдалося оновити читача.");LOG.info("Оновлено читача id="+id);return get(id);}
    public void delete(long id)throws SQLException{get(id);if(!dao.delete(id))throw new ApiException(409,"Читач має бронювання й не може бути видалений.");LOG.info("Видалено читача id="+id);}
    private void validate(Reader r){if(r==null||blank(r.cardNumber())||blank(r.fullName()))throw new ApiException(400,"Номер читацького квитка та ПІБ є обов'язковими.");if(r.email()!=null&&!r.email().isBlank()&&!r.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new ApiException(400,"Некоректний формат електронної пошти.");}
    private boolean blank(String s){return s==null||s.isBlank();}
}
