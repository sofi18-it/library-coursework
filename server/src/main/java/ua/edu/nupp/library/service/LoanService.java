package ua.edu.nupp.library.service;

import ua.edu.nupp.library.dao.LoanDao;
import ua.edu.nupp.library.model.Loan;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

public class LoanService {
    private static final Logger LOG=Logger.getLogger(LoanService.class.getName());
    private final LoanDao dao;
    public LoanService(LoanDao dao){this.dao=dao;}
    public List<Loan> list()throws SQLException{return dao.findAll();}
    public Loan get(long id)throws SQLException{Loan x=dao.findById(id);if(x==null)throw new ApiException(404,"Видачу не знайдено.");return x;}
    public Loan update(long id,Loan request)throws SQLException{Loan current=get(id);if(current.returnedAt()!=null)throw new ApiException(409,"Повернену видачу змінювати не можна.");if(request==null||request.dueDate()==null||request.dueDate().isBefore(current.issuedAt()))throw new ApiException(400,"Термін повернення не може бути раніше дати видачі.");if(!dao.updateDueDate(id,request.dueDate()))throw new ApiException(409,"Не вдалося змінити термін повернення.");LOG.info("Змінено термін видачі id="+id);return get(id);}
    public Loan returnLoan(long id)throws SQLException{get(id);if(!dao.returnLoan(id,LocalDate.now()))throw new ApiException(409,"Видачу не повернуто.");LOG.info("Повернуто видачу id="+id);return get(id);}
    public void delete(long id)throws SQLException{Loan x=get(id);if(x.returnedAt()==null)throw new ApiException(409,"Видаляти можна лише вже повернену видачу.");if(!dao.delete(id))throw new ApiException(409,"Не вдалося видалити видачу.");LOG.info("Видалено видачу id="+id);}
}
