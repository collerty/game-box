// features/triviatoe/TriviatoeQuestionBank.kt
package com.example.gamehub.features.triviatoe

import com.example.gamehub.features.triviatoe.model.TriviatoeQuestion

object TriviatoeQuestionBank {
    val all = listOf(
        TriviatoeQuestion.MultipleChoice(
            question = "What is the capital of France?",
            answers = listOf("Paris", "London", "Berlin", "Madrid"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "2 + 2 = ?",
            answers = listOf("3", "4", "5", "6"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which planet is known as the Red Planet?",
            answers = listOf("Earth", "Mars", "Jupiter", "Venus"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Who wrote 'Romeo and Juliet'?",
            answers = listOf("William Shakespeare", "Charles Dickens", "Jane Austen", "Leo Tolstoy"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the largest mammal in the world?",
            answers = listOf("Elephant", "Blue Whale", "Giraffe", "Great White Shark"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which element has the chemical symbol 'O'?",
            answers = listOf("Gold", "Oxygen", "Osmium", "Iron"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many continents are there?",
            answers = listOf("5", "6", "7", "8"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What year did World War II end?",
            answers = listOf("1943", "1944", "1945", "1946"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the boiling point of water at sea level in Celsius?",
            answers = listOf("100", "0", "50", "212"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which is the longest river in the world?",
            answers = listOf("Amazon", "Nile", "Yangtze", "Mississippi"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Who painted the Mona Lisa?",
            answers = listOf("Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci", "Claude Monet"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which gas do plants absorb from the atmosphere?",
            answers = listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "In which Italian city can you find the Colosseum?",
            answers = listOf("Rome", "Venice", "Naples", "Milan"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the largest canyon in the world?",
            answers = listOf("Verdon Gorge", "Grand Canyon", "King’s Canyon", "Fjaðrárgljúfur Canyon"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How long is the border between the United States and Canada?",
            answers = listOf("3,525 miles", "4,525 miles", "5,525 miles", "6,525 miles"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the largest active volcano in the world?",
            answers = listOf("Mount Etna", "Mount Vesuvius", "Mount Batur", "Mouna Loa"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "In which museum can you find Leonardo Da Vinci’s Mona Lisa?",
            answers = listOf("Le Louvre", "Uffizi Museum", "British Museum", "Metropolitan Museum of Art"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "When did Salt Lake City host the Winter Olympics?",
            answers = listOf("1992", "2002", "1998", "2008"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the largest continent in size?",
            answers = listOf("Africa", "Europe", "Asia", "North America"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which famous inventor invented the telephone?",
            answers = listOf("Thomas Edison", "Benjamin Franklin", "Nikola Tesla", "Alexander Graham Bell"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What does the Richter scale measure?",
            answers = listOf("Earthquake intensity", "Wind Speed", "Temperature", "Tornado Strength"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the longest river in the world?",
            answers = listOf("Amazon River", "Nile", "Yellow River", "Congo River"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country does not share a border with Romania?",
            answers = listOf("Ukraine", "Bulgaria", "Poland", "Hungary"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "When was the first Harry Potter book published?",
            answers = listOf("1999", "2001", "2003", "1997"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "If you are eating chicken jalfrezi, what type of food are you eating?",
            answers = listOf("Indian Food", "French food", "Italian food", "Mexican Food"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What was Euclid?",
            answers = listOf("A philosopher", "A mathematician", "A poet", "A painter"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which ballroom dance originated in Germany and Austria?",
            answers = listOf("Salsa", "Jive", "Waltz", "Cha Cha"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the capital of Iraq?",
            answers = listOf("Islamabad", "Tehran", "Amman", "Baghdad"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "In which country is the baht the currency?",
            answers = listOf("Thailand", "Vietnam", "Malaysia", "Indonesia"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What colour is the “m” from the McDonald’s logo?",
            answers = listOf("Blue", "Yellow", "Red", "Black"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is guacamole made of?",
            answers = listOf("Banana", "Yoghurt", "Avocado", "Chick Pea"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "“Onze” is the French number for?",
            answers = listOf("3", "8", "9", "11"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many players are in a cricket team?",
            answers = listOf("11", "8", "9", "10"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What does NASA stand for?",
            answers = listOf(
                "Nautical And Space Association",
                "National Aeronautics and Space Administration",
                "National Aeronautics and Space Association",
                "New Aeronautics and Spacial Administration"
            ),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Who was the first woman to win a Nobel Prize?",
            answers = listOf("Mother Teresa", "Jane Adams", "Marie Curie", "Alva Myrdal"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the national animal of England?",
            answers = listOf("Puffin", "Rabbit", "Fox", "Lion"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which constellation is on the Australian flag?",
            answers = listOf("The southern cross", "Orion", "Ursa Minor", "Scorpius"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Apart from water, what is the most popular drink in the world?",
            answers = listOf("Coffee", "Tea", "Beer", "Orange Juice"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many bones are there in an adult human body?",
            answers = listOf("186", "286", "206", "306"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Who famously said “Veni, vidi, vici”?",
            answers = listOf("Winston Churchill", "Charles de Gaulle", "Alexander the Great", "Julius Caesar"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which one of the following is the correct spelling?",
            answers = listOf("Maintenance", "Mantenance", "Miantenance", "Maintenence"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What’s Garfield favourite food?",
            answers = listOf("Pizza", "Lasagna", "Burger", "Sandwich"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How high is Mount Everest?",
            answers = listOf("5,849 m", "6,849 m", "8,849 m", "7,849 m"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the highest mountain in Japan?",
            answers = listOf("Mount Tate", "Mount Kita", "Mount Yari", "Mount Fuji"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which chemical element has Ag as a symbol?",
            answers = listOf("Silver", "Gold", "Iron", "Carbon"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many elements are there on the periodic table?",
            answers = listOf("58", "118", "78", "98"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the national language of Canada?",
            answers = listOf("English", "French", "Dutch", "Spanish"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "A la Crecy is a French dish made of what?",
            answers = listOf("Apples", "Tomatoes", "Potatoes", "Carrots"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Brazil is the biggest producer of?",
            answers = listOf("Rice", "Coffee", "Oil", "Chocolate"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which religion has a God specified for each Rain, Money, Children, and Love?",
            answers = listOf("Islam", "Buddism", "Hinduism", "Judaism"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is a tomato?",
            answers = listOf("Seed", "Vegetable", "Herb", "Fruit"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Ancient Egyptian Houses were built of what?",
            answers = listOf("Wood", "Mud", "Rock", "Brick"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which is the most common drink in Europe?",
            answers = listOf("Water", "Beer", "Wine", "Tea"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which two colors mix to form pink?",
            answers = listOf("Grey and red", "White and orange", "White and red", "Yellow and red"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the average weight of the human brain?",
            answers = listOf("14 kilos", "14 grams", "1 kilo", "4 kilos"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the only food that cannot go bad?",
            answers = listOf("Honey", "Dark chocolate", "Peanut butter", "Canned tuna"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the most visited tourist attraction in the world?",
            answers = listOf("Statue of Liberty", "Eiffel Tower", "Great Wall of China", "Colosseum"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What’s the heaviest organ in the human body?",
            answers = listOf("Brain", "Skin", "Liver", "Heart"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which of these EU countries does not use the euro as its currency?",
            answers = listOf("Poland", "Denmark", "Sweden", "All of them"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What type of food holds the world record for being the most stolen around the globe?",
            answers = listOf("Wagyu beef", "Coffee", "Cheese", "Chocolate"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "On average, how many seeds are located on the outside of a strawberry?",
            answers = listOf("100", "400", "500", "200"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which video game character is known for eating dots in a maze?",
            answers = listOf("Mario", "Donkey Kong", "Pac-Man", "Sonic"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the rarest blood type in humans?",
            answers = listOf("O+", "A-", "B+", "AB-"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which planet in our solar system rotates the fastest?",
            answers = listOf("Earth", "Jupiter", "Mars", "Mercury"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the name of Sherlock Holmes’s loyal friend?",
            answers = listOf("Watson", "Lestrade", "Moriarty", "Hudson"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What was the first animal to be cloned?",
            answers = listOf("Sheep", "Dog", "Cat", "Cow"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which artist sang the 2013 hit song 'Royals'?",
            answers = listOf("Lorde", "Billie Eilish", "Adele", "Sia"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the only letter not found in any U.S. state name?",
            answers = listOf("Q", "Z", "X", "J"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which element is a liquid at room temperature?",
            answers = listOf("Mercury", "Lithium", "Helium", "Iron"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What color is a giraffe’s tongue?",
            answers = listOf("Pink", "Blue", "Black", "Purple"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country invented ice cream?",
            answers = listOf("Italy", "China", "USA", "France"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which bone are babies born without?",
            answers = listOf("Kneecap", "Skull", "Rib", "Jaw"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What fruit has its seeds on the outside?",
            answers = listOf("Apple", "Banana", "Strawberry", "Pear"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal can be seen on the Porsche logo?",
            answers = listOf("Horse", "Bull", "Lion", "Eagle"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which city was the first to reach 1 million inhabitants?",
            answers = listOf("London", "Rome", "Paris", "Athens"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the smallest prime number?",
            answers = listOf("1", "2", "3", "0"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal can sleep for up to three years?",
            answers = listOf("Frog", "Bear", "Snail", "Bat"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the name of the fairy in Peter Pan?",
            answers = listOf("Silvermist", "Rosetta", "Tinker Bell", "Iridessa"),
            correctIndex = 2
        )
    )
    val totalCount get() = all.size
}

