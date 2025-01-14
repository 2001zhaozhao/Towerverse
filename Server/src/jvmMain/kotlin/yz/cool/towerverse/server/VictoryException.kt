package yz.cool.towerverse.server

/**
 * An exception that indicates that the player has won the game.
 *
 * The server's game state can now be compared with the information sent from the client.
 */
class VictoryException : RuntimeException()