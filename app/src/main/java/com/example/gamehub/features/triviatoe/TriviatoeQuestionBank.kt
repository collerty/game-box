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
        )
    )
    val totalCount get() = all.size
}

