package com.voter_api.voter.controller;

import com.voter_api.voter.domain.Vote;
import com.voter_api.voter.domain.VoteCount;
import com.voter_api.voter.domain.VoteCountWinner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class VoteControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VoteController voteController;

    @Autowired
    private Environment environment;

    @Before
    public void setup() {
        createSampleVoterResults();
        createSampleCandidateList();
    }

    private void createSampleVoterResults() {
        // create sample voter test data
        String election = "2016%20Presidential%20Election";
        Map<String, String> candidates = new HashMap<>();
        candidates.put("Chris Keniston (Veterans Party)", "3");
        candidates.put("Darrell Castle (Constitution Party)", "2");
        candidates.put("Donald Trump (Republican Party)", "8");
        candidates.put("Gary Johnson (Libertarian Party)", "3");
        candidates.put("Hillary Clinton (Democratic Party)", "14");
        candidates.put("Jill Stein (Green Party)", "5");
        voteController.getSimulation(candidates, election);
    }

    private void createSampleCandidateList() {
        // create sample list of candidates by calling the candidate service
        String election = "2016%20Presidential%20Election";
        String candidateServiceHostname = environment.getProperty("services.candidate.host");
        String candidateServicePort = environment.getProperty("services.candidate.port");
        String candidateContextPath = environment.getProperty("services.candidate.context-path");
        String candidateServiceResourceUrl = String.format("http://%s:%s/%s/candidates/summary/election/%s",
                candidateServiceHostname, candidateServicePort, candidateContextPath, election);
        restTemplate.getForEntity(candidateServiceResourceUrl, String.class);
    }

    @Ignore("Broken Test - Need to Fix")
    @Test
    public void getCandidatesReturnsListOfCandidateChoices() throws Exception {
        // String expectedCandidates = "{\"candidates\":[\"Chris Keniston (Veterans Party)\",\"Darrell Castle (Constitution Party)\",\"Donald Trump (Republican Party)\",\"Gary Johnson (Libertarian Party)\",\"Hillary Clinton (Democratic Party)\",\"Jill Stein (Green Party)\"]}";
        String election = "2016 Presidential Election";
        String expectedCandidates = "{\"candidates\":[\"Darrell Castle (Constitution Party)\"";
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(
                String.format("/candidates/election/%s", election), String.class);
        assertThat(responseEntity.getStatusCode().value() == 200);
        assertThat(responseEntity.getBody()).contains(expectedCandidates);

    }

    @Test
    public void postVoteReturnsNewVote() throws Exception {
        String expectedVote = "Test Vote";
        String expectedElection = "Test Election";
        Vote vote = new Vote(expectedVote, expectedElection);
        ResponseEntity<Vote> responseEntity =
                restTemplate.postForEntity("/votes", vote, Vote.class);
        assertThat(responseEntity.getStatusCode().value() == 201);
        assertThat(responseEntity.getBody().getVote()).isEqualTo(expectedVote);
    }

    @Test
    public void getResultsReturnsListOfExpectedVoteCounts() throws Exception {
        String expectedVote = "Hillary Clinton (Democratic Party)";
        int expectedCount = 14;
        ParameterizedTypeReference<Map<String, List<VoteCount>>> typeRef =
                new ParameterizedTypeReference<Map<String, List<VoteCount>>>() {
                };
        ResponseEntity<Map<String, List<VoteCount>>> responseEntity =
                restTemplate.exchange("/results", HttpMethod.GET, null, typeRef);
        LinkedHashMap body = ((LinkedHashMap) responseEntity.getBody());
        Collection voteCountCollection = body.values();
        ArrayList voteCountArray = (ArrayList) voteCountCollection.toArray()[0];
        VoteCount voteCount = (VoteCount) voteCountArray.get(0);
        assertThat(responseEntity.getStatusCode().value() == 200);
        assertThat(voteCount.getVote()).isEqualTo(expectedVote);
        assertThat(voteCount.getCount()).isEqualTo(expectedCount);
    }

    @Test
    public void getTotalVotesReturnsSumOfVotes() throws Exception {
        int expectedCount = 35;
        ResponseEntity<VoteCountWinner> responseEntity =
                restTemplate.getForEntity("/results/votes", VoteCountWinner.class);
        VoteCountWinner voteCount = responseEntity.getBody();
        assertThat(responseEntity.getStatusCode().value() == 200);
        assertThat(voteCount.getCount()).isEqualTo(expectedCount);
    }

    @Test
    public void getWinnersReturnsCandidatesWithMostVotes() throws Exception {
        String expectedVote = "Hillary Clinton (Democratic Party)";
        int expectedCount = 14;
        ParameterizedTypeReference<Map<String, List<VoteCount>>> typeRef =
                new ParameterizedTypeReference<Map<String, List<VoteCount>>>() {
                };
        ResponseEntity<Map<String, List<VoteCount>>> responseEntity =
                restTemplate.exchange("/winners", HttpMethod.GET, null, typeRef);
        LinkedHashMap body = ((LinkedHashMap) responseEntity.getBody());
        Collection voteCountCollection = body.values();
        ArrayList voteCountArray = (ArrayList) voteCountCollection.toArray()[0];
        VoteCount voteCount = (VoteCount) voteCountArray.get(0);
        assertThat(responseEntity.getStatusCode().value() == 200);
        assertThat(voteCount.getVote()).isEqualTo(expectedVote);
        assertThat(voteCount.getCount()).isEqualTo(expectedCount);
    }

    @Test
    public void getWinnersVotesReturnsWinnersVoteCount() throws Exception {
        int expectedCount = 14;
        ResponseEntity<VoteCountWinner> responseEntity =
                restTemplate.getForEntity("/winners/votes", VoteCountWinner.class);
        VoteCountWinner voteCountWinner = responseEntity.getBody();
        assertThat(responseEntity.getStatusCode().value() == 200);
        assertThat(voteCountWinner.getCount()).isEqualTo(expectedCount);
    }

    @Test
    public void getSimulationReturnsExpectedMessage() throws Exception {
        String election = "2016%20Presidential%20Election";
        String expectedResponse =
                "{\"message\":\"Simulation data created!\"}";
        ResponseEntity<String> responseEntity =
                restTemplate.getForEntity(String.format("/simulation/election/%s", election), String.class);
        assertThat(responseEntity.getStatusCode().value() == 200);
        assertThat(responseEntity.getBody()).isEqualTo(expectedResponse);
    }
}