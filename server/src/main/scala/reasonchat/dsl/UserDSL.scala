package reasonchat.dsl

import cats.Monad
import reasonchat.dsl.UserDSL.UserStore
import reasonchat.models.{Id, User}
import Result.syntax._
import reasonchat.dsl.Result.Result

object UserDSL {
  type UserStore[F[_]] = StoreDSL[F, Id[User], User]
}

class UserDSL[F[_]: Monad](userStore: UserStore[F], logDSL: LogDSL[F]) {

  private def checkNameExist(name: String): Result[F, Boolean] =
    (for {
      allUsers <- userStore.getAll.handleError
      exists = allUsers.exists(_.name == name)
    } yield exists).value

  def addUser(user: User): Result[F, User] = {
    (for {
      _ <- logDSL.trace(s"Adding user ${user.name}").handleError
      exists <- checkNameExist(user.name).handleError
      _ <- if (exists) Result.error(NicknameAlreadyTaken(user.name)).handleError else Result.success(exists).handleError
      newUser <- userStore.put(user.id, user).handleError
    } yield newUser).value
  }

  def getUser(id: Id[User]): Result[F, User] =
    userStore.get(id)

  def deleteUser(id: Id[User]): Result[F, Unit] =
    userStore.delete(id)
}
