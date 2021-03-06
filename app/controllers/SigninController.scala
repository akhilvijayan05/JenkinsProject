package controllers

import javax.inject.Inject

import play.api.cache
import play.api.data._
import play.api.data.Forms._
import play.api.cache.CacheApi
import play.api.mvc.{Action, Controller}
import services._


class SigninController @Inject() (hashing:HashingTrait,cacheService: CacheTrait) extends Controller {

  val userForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserAuthentication.apply)(UserAuthentication.unapply)
  )
  def default = Action {

    Ok(views.html.signin(""))
  }
  def check = Action{implicit request=>

    userForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest("Something went wrong.")
      },
      value => {
        val maybeUser: List[UserDetails] = cacheService.getCache("cache").toList.flatten
         def iterate(ls:List[UserDetails]):UserDetails= {
          ls match {
            case head :: tail if (value.username.equals(head.username) && hashing.checkHash(value.password,head.password)==true) => head
            case head :: Nil if (value.username.equals(head.username) && hashing.checkHash(value.password,head.password)==true) => head
            case head :: tail=>iterate(tail)
            case Nil=>null
          }
        }
        val result=iterate(maybeUser)
        if(result!=null && result.isSuspend==false)
        Redirect(routes.ProfileController.default).withSession("username" -> result.username).flashing("success" -> "Successfull logged in. Your details are...")
        else if(result!=null && result.isSuspend==true)
          Ok(views.html.signin("Sorry you are suspended by the admin"))
        else
        Ok(views.html.signin("Incorrect Username or password !!"))
          /*val maybeUser: Option[UserDetails] = cacheService.getcache("cache").toList.flatten
          maybeUser match {
            case Some(result) if (value.username.equals(result.username) && HashingPassword.checkHash(value.password,result.password)==true) => {
              Redirect(routes.ProfileController.default).withSession("username" -> result.username).flashing("success" -> "Successfull logged in. Your details are...")
            }
            case _=> Ok(views.html.signin("Incorrect Username or password !!"))
          }*/

        /*else
        Ok(views.html.signin("Incorrect Username or password !!"))*/
      }
    )

  }
}
