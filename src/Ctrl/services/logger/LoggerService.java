package Ctrl.services.logger;


public class LoggerService {
    private static Logger logger = new Logger();
    private static Logger getLogger(){
        return logger;
    }
}
