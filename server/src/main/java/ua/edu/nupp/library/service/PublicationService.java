package ua.edu.nupp.library.service;

import ua.edu.nupp.library.dao.PublicationDao;
import ua.edu.nupp.library.model.Publication;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PublicationService {
    private static final Logger LOG=Logger.getLogger(PublicationService.class.getName());
    private final PublicationDao dao;
    public PublicationService(PublicationDao dao){this.dao=dao;}
    public List<Publication> list() throws SQLException{return dao.findAll();}
    public Publication get(long id) throws SQLException{Publication x=dao.findById(id);if(x==null)throw new ApiException(404,"Видання не знайдено.");return x;}
    public Publication create(Publication x) throws SQLException {validate(x);if(dao.findByIsbn(x.isbn())!=null)throw new ApiException(409,"Видання з таким ISBN уже існує.");long id=dao.create(x);LOG.info("Створено видання id="+id);return get(id);}
    public Publication update(long id,Publication x) throws SQLException {get(id);validate(x);Publication duplicate=dao.findByIsbn(x.isbn());if(duplicate!=null&&!duplicate.id().equals(id))throw new ApiException(409,"Видання з таким ISBN уже існує.");if(!dao.update(id,x))throw new ApiException(409,"Не вдалося оновити видання.");LOG.info("Оновлено видання id="+id);return get(id);}
    public void delete(long id) throws SQLException {get(id);if(!dao.delete(id))throw new ApiException(409,"Видання використовується в бронюваннях і не може бути видалене.");LOG.info("Видалено видання id="+id);}
    private void validate(Publication x){if(x==null||blank(x.isbn())||blank(x.title())||blank(x.author()))throw new ApiException(400,"ISBN, назва й автор є обов'язковими.");if(x.publicationYear()<1400||x.publicationYear()>2200)throw new ApiException(400,"Рік видання має бути в діапазоні 1400–2200.");if(x.totalCopies()<0||x.availableCopies()<0||x.availableCopies()>x.totalCopies())throw new ApiException(400,"Кількість примірників має бути невід'ємною, а доступна кількість не може перевищувати загальну.");}
    private boolean blank(String s){return s==null||s.isBlank();}
}
