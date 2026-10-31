// SAMPLE DATA: replace coordinates and texts with real places from your town.
// Later this array will come from the backend (GraphQL) instead of this file.
// quiz.answer is the index (starting at 0) of the correct option.
const GEMS = [
    {
        id: 1,
        name: "The Old Clock Tower",
        hint: "It has been telling the wrong time for decades.",
        lat: 45.0000,
        lon: 25.0000,
        story: "Built in 1894 by the town's watchmakers, the tower was meant to be the most accurate clock in the region.\n\nA storm damaged the mechanism in 1937. Nobody ever agreed on who should pay for the repair, so it still shows a time that is eleven minutes late.",
        quiz: {
            question: "How late does the clock run?",
            options: ["Five minutes", "Eleven minutes", "One hour"],
            answer: 1
        }
    },
    {
        id: 2,
        name: "The Lantern Passage",
        hint: "A narrow alley that lights up before the streets do.",
        lat: 45.0020,
        lon: 25.0015,
        story: "Lamplighters once used this alley as a shortcut, and they lit its lanterns first so they could find their way to the main street.\n\nThe tradition survived: the lanterns here still switch on a few minutes before the rest of the town.",
        quiz: {
            question: "Who first used the alley?",
            options: ["Lamplighters", "Bakers", "Soldiers"],
            answer: 0
        }
    },
    {
        id: 3,
        name: "The Silent Fountain",
        hint: "Water used to flow here, and people still make wishes.",
        lat: 45.0035,
        lon: 24.9990,
        story: "The fountain ran dry during a drought a century ago and never worked again.\n\nPeople kept tossing coins into the empty basin anyway. Every spring, the town council collects them and donates the money to the library.",
        quiz: {
            question: "Where does the coin money go?",
            options: ["The library", "The church", "The council's budget"],
            answer: 0
        }
    }
];