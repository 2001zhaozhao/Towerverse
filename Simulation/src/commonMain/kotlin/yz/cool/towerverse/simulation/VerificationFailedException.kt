package yz.cool.towerverse.simulation

/**
 * An exception that indicates that the server has failed to verify the client's gameplay.
 */
class VerificationFailedException(message: String) : RuntimeException(message)