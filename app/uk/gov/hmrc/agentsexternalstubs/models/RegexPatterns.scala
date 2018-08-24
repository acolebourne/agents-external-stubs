package uk.gov.hmrc.agentsexternalstubs.models
import scala.util.matching.Regex

object RegexPatterns {

  type Matcher = String => Either[String, String]

  val validNinoNoSpaces: Matcher = validate(
    "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$".r)
  val validNinoWithSpaces: Matcher = validate(
    "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?[A-D]?$".r)
  val validArn: Matcher = validate("^[A-Z]ARN[0-9]{7}$".r)
  val validUtr: Matcher = validate("^[0-9]{10}$".r)
  val validMtdbsa: Matcher = validate("^[A-Z0-9]{1,16}$".r)

  val validDate: Matcher =
    validate(
      "^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$".r)

  val validPostcode: Matcher = validate("^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}|BFPO\\s?[0-9]{1,10}$".r)

  def validate(regex: Regex): Matcher =
    value =>
      if (regex.pattern.matcher(value).matches()) Right(value)
      else Left(s"Supplied value $value does not match pattern ${regex.pattern.toString}")

}
