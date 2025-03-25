/*
Support multiple types of card games ✅
The Game class is an abstract class that can be extended to implement different card games.
Poker and Blackjack classes extend Game, demonstrating multiple game support.

Deck should be flexible ✅
The Deck class is designed to support a standard 52-card deck (Suit & Rank enums).
The structure allows easy modifications to support other card games (e.g., Uno, Rummy) by extending the Card and Deck classes.

Allow shuffling, dealing, and resetting the deck ✅
shuffle(): Randomizes card order.
dealCard(): Removes and returns the top card from the deck.
reset(): Recreates the deck and reshuffles it.

Each game should define its own rules ✅
Poker implements a community card system and deals 2 cards per player.
Blackjack assigns cards to both players and a dealer.
The play() method is overridden in each subclass to define game-specific rules.


Suit
Rank
Card
abs Game
Poker game  : dealInitialCards, play

 */





import java.util.*;

enum Suit {
    HEARTS, DIAMONDS, CLUBS, SPADES;
}

enum Rank {
    TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN,
    JACK, QUEEN, KING, ACE;
}

class Card {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}

class Deck {
    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card dealCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No more cards in the deck!");
        }
        return cards.remove(cards.size() - 1);
    }

    public void reset() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }
}

abstract class Game {
    protected Deck deck;
    protected List<Player> players;

    public Game(List<Player> players) {
        this.players = players;
        this.deck = new Deck();
    }

    public abstract void play();

    public void dealInitialCards(int numCards) {
        for (Player player : players) {
            for (int i = 0; i < numCards; i++) {
                player.addCard(deck.dealCard());
            }
        }
    }
}

// Poker class
class Poker extends Game {
    private List<Card> communityCards;

    public Poker(List<Player> players) {
        super(players);
        this.communityCards = new ArrayList<>();
    }

    @Override
    public void play() {
        System.out.println("Starting a Poker game...");
        dealInitialCards(2);
        for (int i = 0; i < 5; i++) {
            communityCards.add(deck.dealCard());
        }
        System.out.println("Community Cards: " + communityCards);
    }
}

class Blackjack extends Game {
    private Player dealer;

    public Blackjack(List<Player> players) {
        super(players);
        this.dealer = new Player("Dealer");
    }

    @Override
    public void play() {
        System.out.println("Starting a Blackjack game...");
        dealInitialCards(2);
        dealer.addCard(deck.dealCard());
        dealer.addCard(deck.dealCard());
        System.out.println("Dealer's Hand: " + dealer.showHand());
    }
}

class Player {
    private String name;
    private List<Card> hand;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public List<Card> showHand() {
        return hand;
    }

    @Override
    public String toString() {
        return name + "'s hand: " + hand;
    }
}

public class Main {
    public static void main(String[] args) {
        List<Player> players = new ArrayList<>();
        players.add(new Player("Alice"));
        players.add(new Player("Bob"));

        Game pokerGame = new Poker(players);
        pokerGame.play();

        System.out.println("---------");

        Game blackjackGame = new Blackjack(players);
        blackjackGame.play();
    }
}



