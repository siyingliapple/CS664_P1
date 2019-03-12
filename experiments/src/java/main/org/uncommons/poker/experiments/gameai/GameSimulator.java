package org.uncommons.poker.experiments.gameai;

import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.poker.game.cards.*;
import org.uncommons.poker.game.rules.PokerRules;
import org.uncommons.poker.game.rules.TexasHoldem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * @author Siying Li
 */
public class GameSimulator
{
    static int cleverAIMoney = 100000;
    static int randomAIMoney = 100000;
    static HashMap<String, Double> probMap = new HashMap<>();

    private static void  getMap() throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader("experiments/src/java/main/org/uncommons/poker/experiments/colddecks/hand_with_prob.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String key = line.split("\t")[0];
                Double value = Double.valueOf(line.split("\t")[1]);
                probMap.put(key, value);
            }
        }
    }

    private static String getStartingHandClassification(PlayingCard card1, PlayingCard card2)
    {
        StringBuilder buffer = new StringBuilder(3);
        // Ensure that the first card is the highest ranked.
        if (card2.compareTo(card1) > 0)
        {
            buffer.append(card2.getValue().getSymbol());
            buffer.append(card1.getValue().getSymbol());
        }
        else
        {
            buffer.append(card1.getValue().getSymbol());
            buffer.append(card2.getValue().getSymbol());
        }

        // If not a pair, is it suited or off-suit?
        if (card1.getValue() != card2.getValue())
        {
            buffer.append(card1.getSuit() == card2.getSuit() ? 's' : 'o');
        }

        return buffer.toString();
    }

    private static boolean getRankFromDifferentRound(ArrayList<PlayingCard> playingCards, ArrayList<PlayingCard> communityCards, int round,  HandRanking r) {


        if(round <= 2){
            ArrayList<PlayingCard> subset = new ArrayList<>();
            subset.add(communityCards.get(0));
            subset.add(communityCards.get(1));
            subset.add(communityCards.get(2));
            subset.add(playingCards.get(0));
            subset.add(playingCards.get(1));

            return (new FiveCardHandEvaluator().evaluate(subset).getRanking().compareTo(r) >=0 );
        }else{

            ArrayList<PlayingCard> subset = new ArrayList<>();
            subset.add(communityCards.get(0));
            subset.add(communityCards.get(1));
            subset.add(communityCards.get(2));
            subset.add(communityCards.get(3));
            subset.add(communityCards.get(4));
            subset.add(playingCards.get(0));
            subset.add(playingCards.get(1));

            return (new SevenCardHandEvaluator().evaluate(subset).getRanking().compareTo(r) >=0 );
        }
    }






    private final HandEvaluator handEvaluator = new SevenCardHandEvaluator();

    static public void startOneGame(int game_index, int small_blind, int ai_level) throws IOException {

        //Blind
        int money_on_the_table = small_blind * 3;

        boolean i_am_bigblind = true;
        boolean i_go_first = true;

        if(game_index % 2 == 0){
            i_am_bigblind = false;
            i_go_first = true;
            cleverAIMoney -= small_blind;
            randomAIMoney -= 2 * small_blind;
        } else{
            i_am_bigblind = true;
            i_go_first = false;
            randomAIMoney -= small_blind;
            cleverAIMoney -= 2 * small_blind;
        }


        Deck deck = Deck.createFullDeck(new MersenneTwisterRNG());

        // Deal the community cards (for the purposes of the experiment, it doesn't
        // matter that we do this before the hole cards).
        ArrayList<PlayingCard> communityCards = new ArrayList<PlayingCard>(5);
        for (int i = 0; i < 5; i++)
        {
            communityCards.add(deck.dealCard());
        }

        PlayingCard holeCard1 = deck.dealCard();
        PlayingCard holeCard2 = deck.dealCard();

        ArrayList<PlayingCard> myCards = new ArrayList<>();
        myCards.add(holeCard1);
        myCards.add(holeCard2);

        ArrayList<PlayingCard> yourCards = new ArrayList<>();
        PlayingCard holeCard3 = deck.dealCard();
        PlayingCard holeCard4 = deck.dealCard();
        yourCards.add(holeCard3);
        yourCards.add(holeCard4);

        PokerRules rules = new TexasHoldem();
        RankedHand myhand = rules.rankHand(myCards, communityCards);
        RankedHand yourhand = rules.rankHand(yourCards, communityCards);


        //Round 1 & 2
        //You have to bet big blind

        for(int round = 1 ; round <= 4  ; round++) {

            int fixBid = 0;
            if(round ==1 || round == 2){
                fixBid = 2 * small_blind;
            } else {
                fixBid = 4 * small_blind;
            }

            //Random A.I. bid randomly
            long bid = Math.round(new Random().nextInt(3));

            //Opponent Bid first
            //A.I fold
            if (game_index % 2 == 1) {
                if (bid == 0) {
                    cleverAIMoney += money_on_the_table;
                    return;
                }

                //Opponent Call
                if (bid == 1) {
                    randomAIMoney -= fixBid;
                    money_on_the_table += fixBid;

                    if (ai_level == 0) {
                        cleverAIMoney -= fixBid;
                        money_on_the_table += fixBid;
                    } else if (ai_level == 1) {
                        if (getProb(myCards) > 0.4) {
                            cleverAIMoney -= fixBid;
                            money_on_the_table += fixBid;
                        } else {
                            randomAIMoney += money_on_the_table;
                            return;
                        }
                    } else if (ai_level == 2){


                        if(getRankFromDifferentRound(myCards,communityCards,round,HandRanking.PAIR)){
                            cleverAIMoney -= fixBid;
                            money_on_the_table += fixBid;
                        } else if(getProb(myCards) > 0.4) {
                            cleverAIMoney -= fixBid;
                            money_on_the_table += fixBid;
                        } else {
                            randomAIMoney += money_on_the_table;
                            return;
                        }
                    }
                }
                //Opponent Raise
                if (bid == 2) {
                    randomAIMoney -= 2 * fixBid;
                    money_on_the_table += 2 * fixBid;
                    if (ai_level == 0) {
                        cleverAIMoney -= 2 * fixBid;
                        money_on_the_table += 2 * fixBid;
                    } else if (ai_level == 1) {
                        if (getProb(myCards) > 0.45) {
                            cleverAIMoney -= 2 * fixBid;
                            money_on_the_table += 2 * fixBid;
                        } else {
                            randomAIMoney += money_on_the_table;
                            return;
                        }
                    }  else if (ai_level == 2){

                        if(getRankFromDifferentRound(myCards,communityCards,round,HandRanking.TWO_PAIR)){
                            cleverAIMoney -= 2 * fixBid;
                            money_on_the_table += 2 * fixBid;
                        } else if(getProb(myCards) > 0.45) {
                            cleverAIMoney -= 2 * fixBid;
                            money_on_the_table += 2 * fixBid;
                        } else {
                            randomAIMoney += money_on_the_table;
                            return;
                        }
                    }
                }
            } else {

                if(ai_level == 0){
                  // I always Call
                  cleverAIMoney -= fixBid;
                  money_on_the_table += fixBid;

                  if( bid == 0){
                      cleverAIMoney += money_on_the_table;
                      return;
                  } else {
                      randomAIMoney -= fixBid;
                      money_on_the_table += fixBid;
                  }

                }

                else if(ai_level == 1) {
                    // I FOLD
                    if (getProb(myCards) < 0.35) {
                        randomAIMoney += money_on_the_table;
                        return;
                        // I Raise
                    } else if (getProb(myCards) > 0.55) {
                        cleverAIMoney -= fixBid * 2;
                        money_on_the_table += fixBid * 2;
                        // Oppopent Fold
                        if (bid == 0) {
                            cleverAIMoney += money_on_the_table;
                            return;
                        }
                        // Opponent
                        if (bid == 1 || bid == 2) {
                            randomAIMoney -= 2 * fixBid;
                            money_on_the_table += 2 * fixBid;
                        }
                        // I Call
                    } else {
                        cleverAIMoney -= small_blind;
                        money_on_the_table += small_blind;
                        // Oppopent Fold
                        if (bid == 0) {
                            cleverAIMoney += money_on_the_table;
                            return;
                        }
                        // Opponent Call
                        if (bid == 1 || bid == 2) {
                            randomAIMoney -= fixBid;
                            money_on_the_table += fixBid;
                        }
                    }
                } else if(ai_level == 2){

                    //I Raise
                    if(getRankFromDifferentRound(myCards,communityCards,round,HandRanking.PAIR)) {
                        cleverAIMoney -= fixBid * 2;
                        money_on_the_table += fixBid * 2;
                        if (bid == 0) {
                            cleverAIMoney += money_on_the_table;
                            return;
                        }
                        // Opponent Call
                        if (bid == 1 || bid == 2) {
                            randomAIMoney -= fixBid * 2;
                            money_on_the_table += fixBid * 2;
                        }
                    //I call
                    } else {
                        cleverAIMoney -= fixBid;
                        money_on_the_table += fixBid;

                        if (bid == 0) {
                            cleverAIMoney += money_on_the_table;
                            return;
                        }
                        // Opponent Call
                        if (bid == 1 || bid == 2) {
                            randomAIMoney -= fixBid;
                            money_on_the_table += fixBid;
                        }
                    }
                }
            }
        }

        if (myhand.compareTo(yourhand) > 0)
            cleverAIMoney += money_on_the_table;
        else if (myhand.compareTo(yourhand) == 0) {
            cleverAIMoney += money_on_the_table / 2;
            randomAIMoney += money_on_the_table / 2;
        } else {
            randomAIMoney += money_on_the_table;
        }


    }

    public static double getProb(ArrayList<PlayingCard> cards) throws IOException {
        return probMap.get(getStartingHandClassification(cards.get(0),cards.get(1)));
    }




    public static void main(String[] args) throws GeneralSecurityException, IOException {
        int total_games = 1000;

        getMap();

//        //AILEVEL = 0 AI Level 0
//        for (int i = 0; i < total_games; i++) {
//            startOneGame(i, 20, 0);
//        }
//
//        System.out.println(cleverAIMoney);
//        System.out.println(randomAIMoney);

        //125800
        //74200
        cleverAIMoney = 100000;
        randomAIMoney = 100000;

        //AILEVEL = 1 AI Level 0 Use probility from opening hand map
//        for (int i = 0; i < total_games; i++) {
//            startOneGame(i, 20, 1);
//        }
//
//        System.out.println(cleverAIMoney);
//        System.out.println(randomAIMoney);

        //153860
        //46140

        //AILEVEL = 2 Make some Rules
        //Always Raise when you have three in a kind

        cleverAIMoney = 100000;
        randomAIMoney = 100000;

        for (int i = 0; i < total_games; i++) {
            startOneGame(i, 20, 2);
        }

        System.out.println(cleverAIMoney);
        System.out.println(randomAIMoney);

        //172270
        //27730

    }
}
