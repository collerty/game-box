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
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the hottest planet in our solar system?",
            answers = listOf("Mercury", "Venus", "Mars", "Jupiter"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the hardest natural substance on Earth?",
            answers = listOf("Gold", "Iron", "Diamond", "Quartz"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Who discovered penicillin?",
            answers = listOf("Marie Curie", "Alexander Fleming", "Louis Pasteur", "Gregor Mendel"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which language has the most native speakers?",
            answers = listOf("English", "Mandarin", "Spanish", "Hindi"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many hearts does an octopus have?",
            answers = listOf("1", "2", "3", "4"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What does the 'www' stand for in a website browser?",
            answers = listOf("World Wide Web", "Wild Wild West", "Wide Web World", "Web World Wide"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which continent is the least populated?",
            answers = listOf("Antarctica", "Australia", "South America", "Europe"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which instrument has keys, pedals and strings?",
            answers = listOf("Guitar", "Violin", "Piano", "Trumpet"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country hosted the 2016 Summer Olympics?",
            answers = listOf("China", "Brazil", "Russia", "UK"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the tallest building in the world (as of 2024)?",
            answers = listOf("Shanghai Tower", "Abraj Al Bait", "Burj Khalifa", "One World Trade Center"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many ribs are in a human body?",
            answers = listOf("12", "20", "24", "30"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the main ingredient in traditional Japanese miso soup?",
            answers = listOf("Soybean paste", "Seaweed", "Rice", "Fish"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the fastest land animal?",
            answers = listOf("Cheetah", "Lion", "Horse", "Gazelle"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the national flower of Japan?",
            answers = listOf("Rose", "Lily", "Cherry Blossom", "Lotus"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which planet has the most moons?",
            answers = listOf("Earth", "Jupiter", "Saturn", "Mars"),
            correctIndex = 2 // As of 2024, Saturn leads!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the smallest country in the world?",
            answers = listOf("Monaco", "Vatican City", "Malta", "Liechtenstein"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the most widely spoken language in South America?",
            answers = listOf("Portuguese", "English", "Spanish", "French"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which sea creature has three hearts?",
            answers = listOf("Dolphin", "Shark", "Octopus", "Whale"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the longest bone in the human body?",
            answers = listOf("Tibia", "Fibula", "Femur", "Humerus"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal is known as the 'King of the Jungle'?",
            answers = listOf("Tiger", "Elephant", "Lion", "Gorilla"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What element does 'K' stand for on the periodic table?",
            answers = listOf("Krypton", "Potassium", "Kelvin", "Kerosene"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which is the largest internal organ in the human body?",
            answers = listOf("Heart", "Liver", "Lung", "Brain"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal cannot jump?",
            answers = listOf("Elephant", "Kangaroo", "Lion", "Rabbit"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country has the most islands in the world?",
            answers = listOf("Canada", "Indonesia", "Sweden", "Philippines"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How long is a day on Venus compared to Earth?",
            answers = listOf("Shorter", "Same", "Longer", "No days"),
            correctIndex = 2 // It's longer than its year!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the only mammal capable of true flight?",
            answers = listOf("Bat", "Flying squirrel", "Sugar glider", "Owl"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which fruit was once known as a 'love apple'?",
            answers = listOf("Tomato", "Apple", "Cherry", "Peach"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What color is hippo sweat?",
            answers = listOf("Blue", "Red", "Green", "Pink"),
            correctIndex = 1 // It's actually red!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the loudest animal on Earth relative to its size?",
            answers = listOf("Howler monkey", "Tiger pistol shrimp", "Lion", "Elephant"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which planet rains diamonds?",
            answers = listOf("Earth", "Neptune", "Jupiter", "Uranus"),
            correctIndex = 3 // Uranus and Neptune
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What was the first toy to be advertised on TV?",
            answers = listOf("Barbie", "Lego", "Mr. Potato Head", "Teddy Bear"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the only food astronauts can’t eat in space?",
            answers = listOf("Bread", "Honey", "Salt", "Pizza"),
            correctIndex = 0 // Bread makes crumbs!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many hearts does an earthworm have?",
            answers = listOf("1", "5", "7", "10"),
            correctIndex = 1 // 5 hearts!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal’s fingerprints are so similar to humans' they can taint crime scenes?",
            answers = listOf("Chimpanzee", "Koala", "Gorilla", "Dog"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the only bird that can fly backwards?",
            answers = listOf("Parrot", "Hummingbird", "Swallow", "Ostrich"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How much of Earth's oxygen is produced by the Amazon rainforest?",
            answers = listOf("5%", "10%", "20%", "50%"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which ocean is the deepest in the world?",
            answers = listOf("Atlantic", "Arctic", "Indian", "Pacific"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the only letter that doesn’t appear in any U.S. state name?",
            answers = listOf("Q", "Z", "X", "J"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal has rectangular pupils?",
            answers = listOf("Cat", "Goat", "Horse", "Rabbit"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What animal can sleep with one eye open?",
            answers = listOf("Horse", "Shark", "Dolphin", "Cow"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal can taste with its feet?",
            answers = listOf("Butterfly", "Frog", "Duck", "Bat"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many years can a snail sleep?",
            answers = listOf("Up to 3", "Up to 1", "Up to 7", "Up to 10"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What does 'Wi-Fi' let you do?",
            answers = listOf("Connect to the internet wirelessly", "Wash dishes", "Write code", "Take photos"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What food did astronauts famously eat on the moon?",
            answers = listOf("Pizza", "Ice Cream", "Cheese", "Bacon"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country once had a king who only reigned for 20 minutes?",
            answers = listOf("France", "Italy", "Spain", "Greece"),
            correctIndex = 1 // Italy, King Umberto II!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which fruit can float on water because it's made of 25% air?",
            answers = listOf("Apple", "Banana", "Watermelon", "Grape"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is the official national animal of Scotland?",
            answers = listOf("Sheep", "Unicorn", "Horse", "Dragon"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which bird is known for dancing to impress a mate?",
            answers = listOf("Penguin", "Peacock", "Blue-footed Booby", "Chicken"),
            correctIndex = 2
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What do you call a group of flamingos?",
            answers = listOf("A bunch", "A flock", "A parade", "A stand"),
            correctIndex = 2 // A parade!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which planet in our solar system smells like rotten eggs?",
            answers = listOf("Venus", "Mars", "Uranus", "Saturn"),
            correctIndex = 3 // Saturn (hydrogen sulfide)!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is banned in public places in Florida after 6pm on a Thursday?",
            answers = listOf("Singing", "Dancing", "Whistling", "Farting"),
            correctIndex = 3 // It's true!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal is known for laughing when tickled?",
            answers = listOf("Dog", "Chimpanzee", "Rat", "Horse"),
            correctIndex = 2 // Rats!
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is a group of crows called?",
            answers = listOf("A murder", "A party", "A school", "A pack"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country invented French fries?",
            answers = listOf("France", "Belgium", "USA", "Canada"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which sea creature can change its color to hide or show off?",
            answers = listOf("Crab", "Octopus", "Shrimp", "Lobster"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "How many noses does a slug have?",
            answers = listOf("1", "2", "3", "4"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal can sleep for three years at a time?",
            answers = listOf("Tortoise", "Snail", "Bat", "Frog"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What is illegal to own as a pet in Switzerland unless you have two of them?",
            answers = listOf("Parrots", "Guinea pigs", "Cats", "Goldfish"),
            correctIndex = 1
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What did the first oranges look like?",
            answers = listOf("Green", "Orange", "Yellow", "Red"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "What animal do you get when you cross a donkey and a zebra?",
            answers = listOf("Zonkey", "Dorse", "ZebraDonk", "Zorilla"),
            correctIndex = 0
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which animal can hold its breath the longest underwater?",
            answers = listOf("Dolphin", "Whale", "Sea turtle", "Sperm whale"),
            correctIndex = 3
        ),
        TriviatoeQuestion.MultipleChoice(
            question = "Which country is known for inventing pizza?",
            answers = listOf("Greece", "Italy", "France", "Turkey"),
            correctIndex = 1
        )
    )
    val totalCount get() = all.size
}

