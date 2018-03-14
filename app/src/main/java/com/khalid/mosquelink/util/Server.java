package com.khalid.mosquelink.util;

public class Server {

    private  String ip = "http://mosfine.org/mosfine/php/";
    private  String retrieveMosqueLocations = ip + "retrieveMosqueLocations.php";
    private  String retrieveMosqueDetail = ip + "retrieveMosqueDetail.php";
    private  String sendMosqueSuggestion= ip + "sendMosqueSuggestion.php";
    private  String sendIncorrectInformation= ip + "sendIncorrectInfo.php";
    private  String sendFeedback= ip + "sendFeedback.php";
    private  String checkBlacklist= ip + "checkBlackList.php";


    public String getSendFeedback() {
        return sendFeedback;
    }

    public  String getRetrieveMosqueLocations() {
        return retrieveMosqueLocations;
    }

    public  String getRetrieveMosqueDetail() {
        return retrieveMosqueDetail;
    }

    public String getSendMosqueSuggestion() {
        return sendMosqueSuggestion;
    }

    public String getSendIncorrectInformation() {
        return sendIncorrectInformation;
    }

    public String getCheckBlacklist() {
        return checkBlacklist;
    }
}
