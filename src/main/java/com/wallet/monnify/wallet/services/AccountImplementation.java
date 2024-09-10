package com.wallet.monnify.wallet.services;

import com.wallet.monnify.config.AppConfigs;
import com.wallet.monnify.user.data.model.User;
import com.wallet.monnify.user.data.repository.UserRepository;
import com.wallet.monnify.utils.Constants;
import com.wallet.monnify.wallet.data.model.Account;
import com.wallet.monnify.wallet.data.repository.AccountRepository;
import com.wallet.monnify.wallet.dto.request.CreateRequest;
import com.wallet.monnify.wallet.dto.response.BalanceResponse;
import com.wallet.monnify.wallet.dto.response.CreateResponse;
import com.wallet.monnify.wallet.dto.response.GetBalanceResponseData;
import com.wallet.monnify.wallet.dto.response.ReservedAccountResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service @AllArgsConstructor @Slf4j
public class AccountImplementation implements IAccount{

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AppConfigs appConfig;


    @Override
    public ReservedAccountResponse createReservedAccount(CreateRequest createRequest) throws Exception {
        createRequest.setAccountReference(generateAccountReference());
        createRequest.setContractCode(appConfig.getMonnifyContractCode());
        CreateResponse responseObject;
        try{
            responseObject = makeApiRequest(createRequest, appConfig.getMonnifyCreateAccountUrl());
        } catch (Exception e){
            throw new Exception(e.getMessage()+ "::From Api Call");
        }

        assert responseObject != null;

        String accountNumber = responseObject.getResponseBody().getAccounts().get(0).getAccountNumber();
        String bankName = responseObject.getResponseBody().getAccounts().get(0).getBankName();

        Account newAccount = buildAccount(responseObject, accountNumber, bankName);
        Account savedAccount = accountRepository.save(newAccount);

        Optional<User> foundUser = userRepository.findUserByEmail(createRequest.getCustomerEmail());
        foundUser.ifPresent(user -> {
            user.setAccount(savedAccount);
            userRepository.save(user);
        });

        log.info("{{}}::", responseObject.getResponseBody());
        return responseObject.getResponseBody();
    }

    private static CreateResponse makeApiRequest(CreateRequest createRequest, String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(createRequest.getToken());
        HttpEntity<Object> httpEntity = new HttpEntity<>(createRequest, httpHeaders);
        log.info("{{}}::", httpEntity.toString());
        CreateResponse responseObject = restTemplate.postForObject(url, httpEntity, CreateResponse.class);
        return responseObject;
    }

    private static Account buildAccount(CreateResponse responseObject, String accountNumber, String bankName) {
        Account newAccount = new Account();
        newAccount.setAccountName(responseObject.getResponseBody().getAccountName());
        newAccount.setAccountNumber(accountNumber);
        newAccount.setAccountReference(responseObject.getResponseBody().getAccountReference());
        newAccount.setBankName(bankName);
        newAccount.setCreatedOn(responseObject.getResponseBody().getCreatedOn());
        newAccount.setReservedAccountType(responseObject.getResponseBody().getReservedAccountType());
        newAccount.setCurrencyCode(responseObject.getResponseBody().getCurrencyCode());
        newAccount.setCustomerEmail(responseObject.getResponseBody().getCustomerEmail());
        newAccount.setCustomerName(responseObject.getResponseBody().getCustomerName());
        newAccount.setCollectionChannel(responseObject.getResponseBody().getCollectionChannel());
        newAccount.setReservationReference(responseObject.getResponseBody().getReservationReference());
        newAccount.setStatus(responseObject.getResponseBody().getStatus());
        newAccount.setBvn(responseObject.getResponseBody().getBvn());
        newAccount.setNin(responseObject.getResponseBody().getNin());

        return newAccount;
    }

    private String generateAccountReference(){
        String prefix = appConfig.getAccountReferencePrefix();
        String characters = Constants.ALPHABETS;
        String digits = Constants.DIGITS;

        Random random = new Random();
        StringBuilder randomString = new StringBuilder(prefix);
        for (int i = 0; i < 3; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }
        for (int i = 0; i < 3; i++) {
            int index = random.nextInt(digits.length());
            randomString.append(digits.charAt(index));
        }
        return randomString.toString();
    }

    @Override
    public ReservedAccountResponse getReservedAccount(String accountReference, String apiToken) throws Exception {
        try {
            return makeApiRequest(accountReference, apiToken, appConfig.getMonnifyGetReservedAccountUrl());
        }  catch (Exception e){
            throw new Exception(e.getMessage()+ "::From Api Call");
        }
    }

    private static ReservedAccountResponse makeApiRequest(String accountReference, String apiToken, String baseUrl) {
        String url = baseUrl + accountReference;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(apiToken);
        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
        log.info("{{}}::", httpEntity.toString());
        CreateResponse responseObject = restTemplate.exchange(url, HttpMethod.GET, httpEntity, CreateResponse.class).getBody();
        assert responseObject != null;
        return responseObject.getResponseBody();
    }

    @Override
    public BalanceResponse getAccountBalance(String accountNumber, String apiToken) throws Exception {
//        Account foundAccount = accountRepository.findByAccountNumber(accountBalanceRequest.getAccountNumber()).orElseThrow(()->new Exception("Account Number not found"));
        String url = ""+accountNumber;
//        RestTemplate restTemplate = new RestTemplate();
//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//        httpHeaders.setBearerAuth(accountBalanceRequest.getToken());
//        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = "";
        String clientSecret = "";
        String credentials = apiKey + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        httpHeaders.setBasicAuth(encodedCredentials);
        log.info("{{}}::", httpHeaders.toString());

        HttpEntity<?> httpEntity = new HttpEntity<>("{}", httpHeaders);
        GetBalanceResponseData responseObject = restTemplate.exchange(url, HttpMethod.GET, httpEntity, GetBalanceResponseData.class).getBody();
        assert responseObject != null;
        log.info("{{}}::", responseObject.getResponse().toString());
//        foundAccount.setBalance(responseObject.getResponse().getBalance());
//        accountRepository.save(foundAccount);
        return responseObject.getResponse();
    }
}
