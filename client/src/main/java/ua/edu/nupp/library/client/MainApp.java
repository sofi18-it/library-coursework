package ua.edu.nupp.library.client;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ua.edu.nupp.library.client.model.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class MainApp extends Application {
    private final LibraryApiClient api=new LibraryApiClient();
    private final Label status=new Label("Готово");

    @Override public void start(Stage stage) {
        TabPane tabs=new TabPane(publicationsTab(stage),readersTab(),bookingsTab(),loansTab());
        BorderPane root=new BorderPane(tabs);root.setBottom(status);BorderPane.setMargin(status,new Insets(6,10,6,10));
        stage.setTitle("Бібліотека — бронювання та видача видань");stage.setMinWidth(1080);stage.setMinHeight(680);stage.setScene(new Scene(root,1220,780));stage.show();
        refreshAll();
    }

    private Tab publicationsTab(Stage stage) {
        TableView<Publication> table=new TableView<>();table.getColumns().addAll(
                column("ID",Publication::id,55),column("ISBN",Publication::isbn,140),column("Назва",Publication::title,220),
                column("Автор",Publication::author,180),column("Видавництво",Publication::publisher,150),
                column("Рік",Publication::publicationYear,65),column("Категорія",Publication::category,130),
                column("Фонд",Publication::totalCopies,65),column("Доступно",Publication::availableCopies,80));
        TextField[] f=fields("ISBN","Назва","Автор","Видавництво","Рік","Категорія","Усього примірників","Доступно");
        ProgressBar progress=new ProgressBar(0);progress.setPrefWidth(180);progress.setVisible(false);
        Button save=new Button("Додати / зберегти зміни"),delete=new Button("Видалити"),clear=new Button("Очистити"),refresh=new Button("Оновити"),importCsv=new Button("Імпорт CSV…");
        save.setOnAction(e->{try{Publication selected=table.getSelectionModel().getSelectedItem();Long id=selected==null?null:selected.id();int year=integer(f[4],"Рік");int total=integer(f[6],"Кількість примірників");int available=integer(f[7],"Доступна кількість");Publication x=new Publication(id,f[0].getText().trim(),f[1].getText().trim(),f[2].getText().trim(),emptyToNull(f[3].getText()),year,emptyToNull(f[5].getText()),total,available);runAsync("Збереження видання…",()->id==null?api.post("/publications",x,Publication.class):api.put("/publications/"+id,x,Publication.class),r->{status.setText("Видання збережено.");loadPublications(table);});}catch(Exception ex){showError(ex);}});
        delete.setOnAction(e->{Publication selected=table.getSelectionModel().getSelectedItem();if(selected==null){showError("Оберіть видання у таблиці.");return;}runAsync("Видалення…",()->{api.delete("/publications/"+selected.id());return true;},r->{clear(f);loadPublications(table);status.setText("Видання видалено.");});});
        clear.setOnAction(e->{table.getSelectionModel().clearSelection();clear(f);});refresh.setOnAction(e->loadPublications(table));
        importCsv.setOnAction(e->chooseAndImport(stage,table,progress,importCsv));
        table.getSelectionModel().selectedItemProperty().addListener((obs,old,x)->{if(x!=null){f[0].setText(x.isbn());f[1].setText(x.title());f[2].setText(x.author());f[3].setText(x.publisher());f[4].setText(String.valueOf(x.publicationYear()));f[5].setText(x.category());f[6].setText(String.valueOf(x.totalCopies()));f[7].setText(String.valueOf(x.availableCopies()));}});
        FlowPane actions=new FlowPane(8,8,save,delete,clear,refresh,importCsv,progress);VBox body=section("Каталог видань",f,actions,table);return tab("Видання",body);
    }

    private Tab readersTab() {
        TableView<Reader> table=new TableView<>();table.getColumns().addAll(column("ID",Reader::id,60),column("Читацький квиток",Reader::cardNumber,190),column("ПІБ",Reader::fullName,260),column("Email",Reader::email,240),column("Телефон",Reader::phone,170));
        TextField[] f=fields("Номер читацького квитка","ПІБ","Email","Телефон");
        Button save=new Button("Додати / зберегти зміни"),delete=new Button("Видалити"),clear=new Button("Очистити"),refresh=new Button("Оновити");
        save.setOnAction(e->{try{Reader selected=table.getSelectionModel().getSelectedItem();Long id=selected==null?null:selected.id();Reader x=new Reader(id,f[0].getText().trim(),f[1].getText().trim(),emptyToNull(f[2].getText()),emptyToNull(f[3].getText()));runAsync("Збереження читача…",()->id==null?api.post("/readers",x,Reader.class):api.put("/readers/"+id,x,Reader.class),r->{status.setText("Читача збережено.");loadReaders(table);});}catch(Exception ex){showError(ex);}});
        delete.setOnAction(e->{Reader x=table.getSelectionModel().getSelectedItem();if(x==null){showError("Оберіть читача у таблиці.");return;}runAsync("Видалення…",()->{api.delete("/readers/"+x.id());return true;},r->{clear(f);loadReaders(table);});});
        clear.setOnAction(e->{table.getSelectionModel().clearSelection();clear(f);});refresh.setOnAction(e->loadReaders(table));
        table.getSelectionModel().selectedItemProperty().addListener((o,old,x)->{if(x!=null){f[0].setText(x.cardNumber());f[1].setText(x.fullName());f[2].setText(x.email());f[3].setText(x.phone());}});
        return tab("Читачі",section("Облік читачів",f,new FlowPane(8,8,save,delete,clear,refresh),table));
    }

    private Tab bookingsTab() {
        TableView<Booking> table=new TableView<>();table.getColumns().addAll(column("ID",Booking::id,55),column("ID видання",Booking::publicationId,90),column("ID читача",Booking::readerId,90),column("Створено",Booking::createdAt,180),column("Стан",Booking::state,155),column("F1 очікування (0–100)",Booking::f1WaitScore,125),column("F2 попит",Booking::f2Demand,90),column("F3 дефіцит",Booking::f3Shortage,95),column("I",Booking::integralIndex,70));
        TextField[] f=fields("ID видання","ID читача");
        Button save=new Button("Створити / зберегти бронювання"),delete=new Button("Видалити"),transition=new Button("Наступний стан"),refresh=new Button("Оновити"),clear=new Button("Очистити");
        save.setOnAction(e->{try{Booking selected=table.getSelectionModel().getSelectedItem();Long id=selected==null?null:selected.id();Booking x=new Booking(id,Long.parseLong(f[0].getText().trim()),Long.parseLong(f[1].getText().trim()),LocalDateTime.now(),"CREATED",0,0,0,0);runAsync("Збереження бронювання…",()->id==null?api.post("/bookings",x,Booking.class):api.put("/bookings/"+id,x,Booking.class),r->{status.setText("Бронювання збережено.");loadBookings(table);});}catch(Exception ex){showError(ex);}});
        delete.setOnAction(e->{Booking x=table.getSelectionModel().getSelectedItem();if(x==null){showError("Оберіть бронювання.");return;}runAsync("Видалення…",()->{api.delete("/bookings/"+x.id());return true;},r->loadBookings(table));});
        transition.setOnAction(e->{Booking x=table.getSelectionModel().getSelectedItem();if(x==null){showError("Оберіть бронювання.");return;}String next=switch(x.state()){case "CREATED"->"CONFIRMED";case "CONFIRMED"->"READY_FOR_ISSUE";case "READY_FOR_ISSUE"->"COMPLETED";default->null;};if(next==null){showError("Життєвий цикл бронювання вже завершений.");return;}runAsync("Зміна стану…",()->api.post("/bookings/"+x.id()+"/transition",Map.of("state",next),Booking.class),r->{status.setText("Новий стан: "+r.state()+"; інтегральний показник I="+String.format(Locale.ROOT,"%.2f",r.integralIndex()));loadBookings(table);loadLoansFromBookingTab();});});
        refresh.setOnAction(e->loadBookings(table));clear.setOnAction(e->{table.getSelectionModel().clearSelection();clear(f);});
        table.getSelectionModel().selectedItemProperty().addListener((o,old,x)->{if(x!=null){f[0].setText(String.valueOf(x.publicationId()));f[1].setText(String.valueOf(x.readerId()));}});
        Label lifecycle=new Label("Життєвий цикл: CREATED → CONFIRMED → READY_FOR_ISSUE → COMPLETED. Перевірка стану та показника виконується на сервері.");lifecycle.setWrapText(true);
        VBox body=section("Бронювання",f,new FlowPane(8,8,save,delete,transition,clear,refresh),table);body.getChildren().add(1,lifecycle);return tab("Бронювання",body);
    }

    private Tab loansTab() {
        TableView<Loan> table=new TableView<>();table.getColumns().addAll(column("ID",Loan::id,65),column("ID бронювання",Loan::bookingId,140),column("Дата видачі",Loan::issuedAt,160),column("Повернути до",Loan::dueDate,160),column("Повернено",Loan::returnedAt,160));
        TextField[] f=fields("Новий термін повернення (YYYY-MM-DD)");
        Button save=new Button("Змінити термін"),returned=new Button("Зареєструвати повернення"),delete=new Button("Видалити повернену видачу"),refresh=new Button("Оновити");
        save.setOnAction(e->{Loan x=table.getSelectionModel().getSelectedItem();if(x==null){showError("Оберіть видачу.");return;}try{Loan request=new Loan(x.id(),x.bookingId(),x.issuedAt(),LocalDate.parse(f[0].getText().trim()),x.returnedAt());runAsync("Оновлення видачі…",()->api.put("/loans/"+x.id(),request,Loan.class),r->loadLoans(table));}catch(Exception ex){showError(ex);}});
        returned.setOnAction(e->{Loan x=table.getSelectionModel().getSelectedItem();if(x==null){showError("Оберіть видачу.");return;}runAsync("Реєстрація повернення…",()->api.post("/loans/"+x.id()+"/return",Map.of(),Loan.class),r->{status.setText("Примірник повернено.");loadLoans(table);loadPublicationsFromLoanTab();});});
        delete.setOnAction(e->{Loan x=table.getSelectionModel().getSelectedItem();if(x==null){showError("Оберіть видачу.");return;}runAsync("Видалення…",()->{api.delete("/loans/"+x.id());return true;},r->loadLoans(table));});
        refresh.setOnAction(e->loadLoans(table));table.getSelectionModel().selectedItemProperty().addListener((o,old,x)->{if(x!=null)f[0].setText(String.valueOf(x.dueDate()));});
        Label notice=new Label("Видача створюється під час переходу бронювання у COMPLETED. Повернення автоматично збільшує доступну кількість примірників.");notice.setWrapText(true);
        VBox body=section("Видачі",f,new FlowPane(8,8,save,returned,delete,refresh),table);body.getChildren().add(1,notice);return tab("Видачі",body);
    }

    private VBox section(String title,TextField[] fields,Pane actions,TableView<?> table) {
        Label heading=new Label(title);heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");FlowPane form=new FlowPane(8,8);form.setPadding(new Insets(4,0,8,0));form.getChildren().addAll(fields);
        VBox box=new VBox(8,heading,form,actions,table);box.setPadding(new Insets(14));VBox.setVgrow(table,Priority.ALWAYS);return box;
    }
    private TextField[] fields(String... prompts){TextField[] result=new TextField[prompts.length];for(int i=0;i<prompts.length;i++){result[i]=new TextField();result[i].setPromptText(prompts[i]);result[i].setPrefColumnCount(Math.max(8,Math.min(22,prompts[i].length()+1)));}return result;}
    private <T,V> TableColumn<T,V> column(String title,Function<T,V> value,double width){TableColumn<T,V> c=new TableColumn<>(title);c.setCellValueFactory(cell->new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));c.setPrefWidth(width);return c;}
    private Tab tab(String title,Pane content){Tab t=new Tab(title,content);t.setClosable(false);return t;}
    private int integer(TextField f,String name){try{return Integer.parseInt(f.getText().trim());}catch(Exception e){throw new IllegalArgumentException("Поле «"+name+"» має містити ціле число.");}}
    private String emptyToNull(String value){return value==null||value.isBlank()?null:value.trim();}
    private void clear(TextField[] fields){for(TextField f:fields)f.clear();}

    private <T> void runAsync(String message,Callable<T> action,Consumer<T> onSuccess){
        status.setText(message);
        Task<T> task=new Task<>(){@Override protected T call()throws Exception{return action.call();}};
        task.setOnSucceeded(e->{status.setText("Готово");onSuccess.accept(task.getValue());});
        task.setOnFailed(e->{status.setText("Операція не виконана");showError(task.getException());});
        Thread worker=new Thread(task,"library-http-operation");worker.setDaemon(true);worker.start();
    }
    private void refreshAll(){TabPane tabs=(TabPane)((BorderPane)status.getScene().getRoot()).getCenter();for(Tab tab:tabs.getTabs())switch(tab.getText()){case "Видання"->{TableView<Publication> table=findTable(tab,Publication.class);if(table!=null)loadPublications(table);}case "Читачі"->{TableView<Reader> table=findTable(tab,Reader.class);if(table!=null)loadReaders(table);}case "Бронювання"->{TableView<Booking> table=findTable(tab,Booking.class);if(table!=null)loadBookings(table);}case "Видачі"->{TableView<Loan> table=findTable(tab,Loan.class);if(table!=null)loadLoans(table);}default->{}}}
    private void loadPublications(TableView<Publication> table){runAsync("Завантаження каталогу…",()->api.getList("/publications",Publication.class),rows->{if(table!=null)table.setItems(FXCollections.observableArrayList(rows));});}
    private void loadReaders(TableView<Reader> table){runAsync("Завантаження читачів…",()->api.getList("/readers",Reader.class),rows->{if(table!=null)table.setItems(FXCollections.observableArrayList(rows));});}
    private void loadBookings(TableView<Booking> table){runAsync("Завантаження бронювань…",()->api.getList("/bookings",Booking.class),rows->{if(table!=null)table.setItems(FXCollections.observableArrayList(rows));});}
    private void loadLoans(TableView<Loan> table){runAsync("Завантаження видач…",()->api.getList("/loans",Loan.class),rows->{if(table!=null)table.setItems(FXCollections.observableArrayList(rows));});}
    private void loadLoansFromBookingTab(){for(Tab t:((TabPane)((BorderPane)getPrimaryStage().getScene().getRoot()).getCenter()).getTabs())if("Видачі".equals(t.getText())){TableView<Loan> loans=findTable(t,Loan.class);if(loans!=null)loadLoans(loans);}}
    private void loadPublicationsFromLoanTab(){for(Tab t:((TabPane)((BorderPane)getPrimaryStage().getScene().getRoot()).getCenter()).getTabs())if("Видання".equals(t.getText())){TableView<Publication> p=findTable(t,Publication.class);if(p!=null)loadPublications(p);}}
    @SuppressWarnings("unchecked") private <T> TableView<T> findTable(Tab tab,Class<T> type){for(javafx.scene.Node child:((VBox)tab.getContent()).getChildren())if(child instanceof TableView<?> table)return (TableView<T>)table;return null;}
    private Stage getPrimaryStage(){return (Stage)status.getScene().getWindow();}

    private void chooseAndImport(Stage stage,TableView<Publication> table,ProgressBar progress,Button button){
        FileChooser chooser=new FileChooser();chooser.setTitle("Виберіть CSV-каталог видань");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-файли","*.csv"));java.io.File selected=chooser.showOpenDialog(stage);if(selected==null)return;Path path=selected.toPath();
        Task<ImportSummary> task=new Task<>(){@Override protected ImportSummary call()throws Exception{return importPublications(path,(done,total)->updateProgress(done,total),this::updateMessage);}};
        progress.progressProperty().bind(task.progressProperty());progress.setVisible(true);button.setDisable(true);status.setText("Підготовка CSV-імпорту…");
        task.setOnSucceeded(e->{status.setText("CSV-імпорт завершено.");progress.progressProperty().unbind();progress.setVisible(false);button.setDisable(false);loadPublications(table);ImportSummary result=task.getValue();String details=result.errors().isEmpty()?"": "\nПомилки:\n"+String.join("\n",result.errors());new Alert(Alert.AlertType.INFORMATION,"Імпортовано: "+result.imported()+"; пропущено: "+result.failed()+details,ButtonType.OK).showAndWait();});
        task.setOnFailed(e->{progress.progressProperty().unbind();progress.setVisible(false);button.setDisable(false);status.setText("CSV-імпорт не виконано");showError(task.getException());});Thread worker=new Thread(task,"library-csv-import");worker.setDaemon(true);worker.start();
    }
    private ImportSummary importPublications(Path path,java.util.function.BiConsumer<Long,Long> progress,Consumer<String> message)throws Exception {
        long total;try(var lines=Files.lines(path,StandardCharsets.UTF_8)){total=Math.max(0,lines.skip(1).filter(s->!s.isBlank()).count());}
        int imported=0,failed=0,processed=0;List<String> errors=new ArrayList<>();
        try(BufferedReader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {
            String header=reader.readLine();if(header==null)throw new IllegalArgumentException("CSV-файл порожній.");List<String> columns=CsvRows.parse(header.replace("\uFEFF",""));List<String> expected=List.of("isbn","title","author","publisher","publication_year","category","total_copies");if(!columns.equals(expected))throw new IllegalArgumentException("Очікувані стовпці: "+String.join(",",expected));
            String line;int lineNo=1;while((line=reader.readLine())!=null){lineNo++;if(line.isBlank())continue;try{List<String> row=CsvRows.parse(line);if(row.size()!=7)throw new IllegalArgumentException("Очікується 7 колонок.");int year=Integer.parseInt(row.get(4));int copies=Integer.parseInt(row.get(6));if(row.get(0).isBlank()||row.get(1).isBlank()||row.get(2).isBlank()||copies<0)throw new IllegalArgumentException("Порожнє обов'язкове поле або від'ємна кількість примірників.");Publication p=new Publication(null,row.get(0),row.get(1),row.get(2),emptyToNull(row.get(3)),year,emptyToNull(row.get(5)),copies,copies);api.post("/publications",p,Publication.class);imported++;}catch(Exception ex){failed++;errors.add("Рядок "+lineNo+": "+ex.getMessage());}processed++;progress.accept(total==0?1L:(long)processed,total==0?1L:total);message.accept("Імпорт CSV: "+processed+" / "+total);}
        }
        return new ImportSummary(imported,failed,errors);
    }
    private record ImportSummary(int imported,int failed,List<String> errors) { }
    private void showError(Throwable e){showError(e==null?"Невідома помилка.":Objects.toString(e.getMessage(),e.getClass().getSimpleName()));}
    private void showError(String message){new Alert(Alert.AlertType.ERROR,message,ButtonType.OK).showAndWait();}
    public static void main(String[] args){launch(args);}
}
