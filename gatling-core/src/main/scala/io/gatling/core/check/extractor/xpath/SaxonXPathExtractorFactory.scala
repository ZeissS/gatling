/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.check.extractor.xpath

import scala.collection.JavaConversions._

import io.gatling.core.check.extractor._
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import net.sf.saxon.s9api.XdmNode

class SaxonXPathExtractorFactory(implicit val saxon: Saxon) extends CriterionExtractorFactory[Option[XdmNode], (String, List[(String, String)])]("xpath") {

  implicit def defaultSingleExtractor = new SingleExtractor[Option[XdmNode], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[XdmNode], criterion: (String, List[(String, String)]), occurrence: Int): Validation[Option[String]] = {
      val (path, namespaces) = criterion
      val result = for {
        text <- prepared
        // XdmValue is an Iterable, so toSeq is a Stream
        result <- saxon.evaluateXPath(path, namespaces, text).toSeq.lift(occurrence)
      } yield result.getStringValue

      result.success
    }
  }

  implicit def defaultMultipleExtractor = new MultipleExtractor[Option[XdmNode], (String, List[(String, String)]), String] {
    override def extract(prepared: Option[XdmNode], criterion: (String, List[(String, String)])): Validation[Option[Seq[String]]] = {
      val (path, namespaces) = criterion
      val result = for {
        node <- prepared
        items <- saxon.evaluateXPath(path, namespaces, node).iterator.map(_.getStringValue).toVector.liftSeqOption
      } yield items

      result.success
    }
  }

  implicit val defaultCountExtractor = new CountExtractor[Option[XdmNode], (String, List[(String, String)])] {
    override def extract(prepared: Option[XdmNode], criterion: (String, List[(String, String)])): Validation[Option[Int]] = {
      val (path, namespaces) = criterion
      val count = prepared.map(saxon.evaluateXPath(path, namespaces, _).size).getOrElse(0)
      Some(count).success
    }
  }
}
