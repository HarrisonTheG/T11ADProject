package sg.edu.iss.ad.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sg.edu.iss.ad.model.*;
import sg.edu.iss.ad.repository.CandleHistoryRepository;
import sg.edu.iss.ad.repository.UserCandleWatchListRepository;
import sg.edu.iss.ad.repository.UserStockWatchListRepository;
import sg.edu.iss.ad.service.CandleService;
import sg.edu.iss.ad.service.UserService;
import sg.edu.iss.ad.service.UserStockWatchListService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class CandleScheduleTask {

    private CandleService candleService;
    private UserCandleWatchListRepository userCandleWatchListRepository;
    private CandleHistoryRepository candleHistoryRepository;

    @Autowired
    public void setCandleService(CandleService cs) {
        this.candleService = cs;
    }

    @Autowired
    public void setUserCandleWatchListRepository(UserCandleWatchListRepository ucwlrepo) {
        this.userCandleWatchListRepository = ucwlrepo;
    }


    @Autowired
    public void setCandleHistoryRepository(CandleHistoryRepository chrepo){
        this.candleHistoryRepository = chrepo;
    }

    private void checkCandle(UserCandleWatchList userCandleWatchList) throws ParseException {
        UserStockWatchList currentUserStockWatchList = userCandleWatchList.getUserStockWatchList();
        User currentUser = currentUserStockWatchList.getUser();
        String currentEmail = currentUser.getEmail();
        String currentTicker = currentUserStockWatchList.getStock().getStockTicker();
        List<CandleModel> result = candleService.getCandleData(currentTicker);
        List<Long> dates;
        MailVo mailVo = new MailVo("PCXGudrew@163.com",currentEmail,"","");

        /*
         * check if the candle exists and send Email
         * */
        if (userCandleWatchList.getCandle().getId() == 1){
            dates = candleService.getBullishEngulfingCandleSignalUNIX(result);
            updateCandleHistory(dates,currentUserStockWatchList.getStock(),userCandleWatchList.getCandle());
            sendNotification(dates,mailVo);
        }
        else if(userCandleWatchList.getCandle().getId() == 2){
            dates = candleService.getBearishEngulfingCandleSignalUNIX(result);
            updateCandleHistory(dates,currentUserStockWatchList.getStock(),userCandleWatchList.getCandle());
            sendNotification(dates,mailVo);
        }
        else if(userCandleWatchList.getCandle().getId() == 3){
            dates = candleService.getMorningStarCandleUNIX(result);
            updateCandleHistory(dates,currentUserStockWatchList.getStock(),userCandleWatchList.getCandle());
            sendNotification(dates,mailVo);
        }
        else{
            dates = candleService.getEveningStarUNIX(result);
            updateCandleHistory(dates,currentUserStockWatchList.getStock(),userCandleWatchList.getCandle());
            sendNotification(dates,mailVo);
        }
    }

    private void sendNotification(List<Long> dates,MailVo mailVo) throws ParseException {
        Long latestCandleDate = dates.get(0);

        //check the latestDate and now, if less and a day, sendEmail
        if (new Date().getTime()/1000-latestCandleDate<86400){
            candleService.sendEmailNotification(mailVo);
        }
    }

    private void updateCandleHistory(List<Long> dates, Stock stock, Candle candle){
        Long date = dates.get(0);
        CandleHistory candleHistoryResult = candleHistoryRepository.getCandleHistoryByStockAndCandleAndTime(stock.getId(),candle.getId(),date);
        if (candleHistoryResult == null){
            /*
            * save candle
            * */
            CandleHistory candleHistory = new CandleHistory();
            candleHistory.setCandle(candle);
            candleHistory.setStock(stock);
            candleHistory.setDateTimeTrigger(date);
            candleHistoryRepository.save(candleHistory);
        }
    }
}
