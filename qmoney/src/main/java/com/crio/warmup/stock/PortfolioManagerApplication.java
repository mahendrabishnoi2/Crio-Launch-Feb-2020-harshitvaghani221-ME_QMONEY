
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Copy the relevant code from #mainReadQuotes to parse the Json into
  // PortfolioTrade list and
  // Get the latest quotes from TIingo.
  // Now That you have the list of PortfolioTrade And their data,
  // With this data, Calculate annualized returns for the stocks provided in the
  // Json
  // Below are the values to be considered for calculations.
  // buy_price = open_price on purchase_date and sell_value = close_price on
  // end_date
  // startDate and endDate are already calculated in module2
  // using the function you just wrote #calculateAnnualizedReturns
  // Return the list of AnnualizedReturns sorted by annualizedReturns in
  // descending order.
  // use gralde command like below to test your code
  // ./gradlew run --args="trades.json 2020-01-01"
  // ./gradlew run --args="trades.json 2019-07-01"
  // ./gradlew run --args="trades.json 2019-12-03"
  // where trades.json is your json file

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) throws 
      IOException, URISyntaxException {

    File file = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    List<PortfolioTrade> allJsonObjects = objectMapper.readValue(file, new 
        TypeReference<List<PortfolioTrade>>() {});
    List<AnnualizedReturn> annualizedReturnList = new ArrayList<>();

    for (PortfolioTrade portfolioTrade : allJsonObjects) {
      List<Double> price = getPrice(objectMapper, portfolioTrade, args);
      annualizedReturnList
          .add(calculateAnnualizedReturns(LocalDate.parse(args[1]), portfolioTrade,
          price.get(0), price.get(1)));
    }
    return annualizedReturnList;
  }

  public static List<Double> getPrice(ObjectMapper objectMapper, 
      PortfolioTrade portfolioTrade, String[] args)
      throws IOException, URISyntaxException {

    RestTemplate restTemplate = new RestTemplate();
    List<Double> mappingList = new ArrayList<>();
    String endDate;
    if (args[1] != null || args[1].equals("")) {
      endDate = args[1];
    } else {
      endDate = LocalDate.now().toString();
    }
      
      
    String uri = "https://api.tiingo.com/tiingo/daily/" + portfolioTrade.getSymbol() 
        + "/prices?startDate="
        + portfolioTrade.getPurchaseDate() + "&endDate=" + endDate + "&token="
        + "29e143088514049c9860ceb299396fe4dfcf095f";
    String result = (restTemplate.getForObject(uri, String.class));
    List<TiingoCandle> candleList = objectMapper.readValue(result, new 
        TypeReference<List<TiingoCandle>>() {});
    mappingList.add(candleList.get(0).getOpen());
    mappingList.add(candleList.get(candleList.size() - 1).getClose());
    return mappingList;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // annualized returns should be calculated in two steps -
  // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value
  // Store the same as totalReturns
  // 2. calculate extrapolated annualized returns by scaling the same in years
  // span. The formula is
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  // Store the same as annualized_returns
  // return the populated list of AnnualizedReturn for all stocks,
  // Test the same using below specified command. The build should be successful
  // ./gradlew test --tests
  // PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, 
      PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;

    double day = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    Double annualizedReturns = Math.pow((1 + totalReturn), (365 / day)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  }

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    File file = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    List<PortfolioTrade> allJsonObjects = objectMapper.readValue(file, new 
        TypeReference<List<PortfolioTrade>>() {});
    List<String> allSymbols = new ArrayList<String>();
    for (PortfolioTrade obj : allJsonObjects) {
      allSymbols.add(obj.getSymbol());
    }
    return allSymbols;
  }

  public static List<TotalReturnsDto> getSortedClosingPrice(ObjectMapper objectMapper,
      List<PortfolioTrade> allJsonObjects, String[] args) throws IOException, URISyntaxException {
    RestTemplate restTemplate = new RestTemplate();
    List<TotalReturnsDto> mappingList = new ArrayList<TotalReturnsDto>();
    for (PortfolioTrade obj : allJsonObjects) {
      String uri = "https://api.tiingo.com/tiingo/daily/" + obj.getSymbol() + "/prices?startDate="
          + obj.getPurchaseDate() + "&endDate=" + args[1] + "&token=" 
          + "29e143088514049c9860ceb299396fe4dfcf095f";
      String result = (restTemplate.getForObject(uri, String.class));
      List<TiingoCandle> candleList = objectMapper.readValue(result, new 
          TypeReference<List<TiingoCandle>>() {});

      TiingoCandle candleObj = candleList.get(candleList.size() - 1);
      TotalReturnsDto trDto = new TotalReturnsDto(obj.getSymbol(), candleObj.getClose());
      mappingList.add(trDto);
    }
    return mappingList;
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, 
          URISyntaxException {
    File file = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    List<PortfolioTrade> allJsonObjects = objectMapper.readValue(file, new 
        TypeReference<List<PortfolioTrade>>() {});
    List<String> allSymbols = new ArrayList<String>();
    List<TotalReturnsDto> mappingList = getSortedClosingPrice(objectMapper, allJsonObjects, args);
    Collections.sort(mappingList);
    for (TotalReturnsDto trDto : mappingList) {
      allSymbols.add(trDto.getSymbol());
    }
    return allSymbols;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader()
        .getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = 
        "/home/crio-user/workspace/thummarm097-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@373ebf74";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplication.mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "21";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, 
        toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateSingleReturn(args));

  }
}
