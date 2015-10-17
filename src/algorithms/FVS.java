package algorithms;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class FVS {

  /**
   * Renvoie une 2-approximation du FVS du graphe formé par les points donné en
   * paramètre. La relation entre les points (voisinage) est donné par la
   * fonction {@link Evaluation#neighbor(Point, ArrayList) neighbor}.
   * 
   * @param points
   *          L'ensemble de points considéré, correspond à l'ensemble "V" de
   *          l'article.
   * @return Une 2-Approximation du Feedback Vertex Set de cet ensemble
   */
  public static ArrayList<Point> compute(ArrayList<Point> points) {

    if(points == null){
      return null;
    }
    
    /*
     * Initialisations
     */

    // Copie des points d'origine, utile pour la derniere partie de l'algorithme
    ArrayList<Point> origine = new ArrayList<>(points);

    // "F" de l'article, contiendra l'ensemble des points résultat.
    ArrayList<Point> ensembleResultat = new ArrayList<>();

    // "w" de l'article. On utilise une Map qui fait correspondre à chaque
    // point son poids.
    // On initialise ici le poids de chaque point à 1.
    HashMap<Point, Double> poidsPoints = new HashMap<>();
    for (Point p : points) {
      poidsPoints.put(p, 1.0);
    }

    // "STACK" de l'article, contiendra les mêmes points que l'ensemble
    // résultat, mais utile pour garder une notion de l'ordre
    // dans lequel les points ont été insérés.
    Stack<Point> pile = new Stack<Point>();

    double gamma;

    // On effectue une première passe pour supprimer les points
    // de degré <= 1.
    cleanUp(points);

    /*
     * Boucle Principale
     */

    while (!points.isEmpty()) {

      // Eventuel cycle semidisjoint
      ArrayList<Point> cycleSemiDisjoint = trouveCycleSemiDisjoint(points);

      // S'il existe un cycle semidisjoint
      if (cycleSemiDisjoint != null) {
        // On cherche gamma, le poids minimum du cycle
        gamma = Double.MAX_VALUE;
        for (Point p : cycleSemiDisjoint) {
          double poids = poidsPoints.get(p);
          if (poids < gamma) {
            gamma = poids;
          }
        }
        // Mise a jour des poids dans le cycle
        for (Point p : cycleSemiDisjoint) {
          poidsPoints.put(p, poidsPoints.get(p) - gamma);
        }

      }
      // Il n'existe pas de cycle semidisjoint
      else {
        // Gamma est le plus petit ratio poids / degre - 1
        gamma = Double.MAX_VALUE;
        for (Point p : points) {
          double ratio = poidsPoints.get(p) / (degre(p, points) - 1);
          if (ratio < gamma) {
            gamma = ratio;
          }
        }

        // Mise a jour des poids des points
        for (Point p : points) {
          poidsPoints.put(p, poidsPoints.get(p) - gamma * (degre(p, points) - 1));
        }
      }
      // Suppression des poids nuls
      for (int i = 0; i < points.size(); ++i) {
        Point p = points.get(i);
        if (poidsPoints.get(p) == 0) {
          // On l'enlève de l'ensemble initial
          points.remove(p);
          // On l'ajoute à la pile et à l'ensemble résultat
          pile.push(p);
          ensembleResultat.add(p);
          // On a enlevé un point, il faut donc revenir en arrière dans
          // le parcours de la liste
          i--;
        }
      }

      // Après suppression des points, on a supprimé des voisins, il faut
      // donc vérifier qu'il n'y a pas de points isolés.
      cleanUp(points);

    }

    /*
     * Boucle d'affinage de l'ensemble résultat On cherche à enlever des points
     * de l'ensemble résultat qui ne seraient pas utiles
     */

    // Parcours des points de l'ensemble dans l'ordre inverse duquel
    // ils ont été insérés
    while (!pile.isEmpty()) {
      Point p = pile.pop();
      // Si sans p l'ensemble est encore un FVS, alors p est inutile
      if (ensemblePriveDePestValide(ensembleResultat, p, origine)) {
        ensembleResultat.remove(p);
      }
    }

    return ensembleResultat;

  }

  /**
   * Vérifie que si l'on enlève un point de l'ensemble, alors cet ensemble est
   * oui ou non encore un FVS.
   * 
   * @param ensemble
   *          Le FVS d'origine
   * @param p
   *          Le point que l'on enlève du FVS
   * @param points
   *          L'ensemble de points du graphe, utile à la verification
   * @return {@code true} si {@code ensemble} \ {{@code p}} est un FVS de
   *         {@code points}, {@code false} sinon.
   */
  private static boolean ensemblePriveDePestValide(ArrayList<Point> ensemble, Point p, ArrayList<Point> points) {
    // On enleve P de l'ensemble
    ArrayList<Point> ensembleSansP = new ArrayList<>(ensemble);
    ensembleSansP.remove(p);
    // Et on vérifie que c'est un FVS valide
    return Evaluation.isValide(points, ensembleSansP);
  }

  /**
   * Enlève tous les points de degré inférieur ou égal à 1 de l'ensemble de
   * points passé en paramètres.
   * 
   * @param points
   *          L'ensemble de points à traiter
   */
  private static void cleanUp(ArrayList<Point> points) {
    for (int i = 0; i < points.size(); ++i) {
      Point p = points.get(i);
      if (degre(p, points) <= 1) {
        points.remove(p);
        i--;
      }
    }
  }

  /**
   * Recherche d'un cycle semi-disjoint dans l'ensemble de points passé en
   * paramètre.
   * 
   * @param points
   *          L'ensemble de points à traiter
   * @return Une {@code ArrayList} des points du cycle semi-disjoint si on en
   *         trouve un, {@code null} sinon.
   */
  private static ArrayList<Point> trouveCycleSemiDisjoint(ArrayList<Point> points) {
    // Liste des points que l'on a visité pendant le parcours du graphe
    ArrayList<Point> visites = new ArrayList<Point>();
    // L'ensemble que l'on va renvoyer.
    ArrayList<Point> res = null;

    // On parcourt tous les points, pour trouver d'éventuels parties disjointes
    // du graphe vu que l'on parcourt le graphe de voisins en voisins.
    for (int i = 0; i < points.size(); ++i) {
      // Si un appel à la fonction récursive n'a pas déjà visité ce point, il va
      // falloir lancer un parcours à partir de ce point.
      if (!visites.contains(points.get(i))) {
        res = trouveCycleSemiDisjointRec(points.get(i), null, visites, new ArrayList<Point>(), points);

        // Si l'appel ci-dessus a renvoyé un cycle, on le renvoie immédiatement.
        if (res != null) {
          return res;
        }
      }
    }
    // Pas de cycle trouvé.
    return null;
  }

  /**
   * Fonction récursive interne à la méthode
   * {@link #trouveCycleSemiDisjoint(ArrayList)}, qui effectue un parcours en
   * profondeur du graphe, et détecte un éventuel cycle semi-disjoint
   * 
   * @param point
   *          Le point à traiter
   * @param pointPrecedent
   *          Le point précédemment parcouru (parent de {@code point} dans le
   *          parcours}
   * @param visites
   *          L'ensemble des points que l'on a visité jusqu'à présent
   * @param chemin
   *          Le chemin correspondant au parcours effecuté
   * @param points
   *          L'ensemble des points du graphe
   * @return Une {@code ArrayList} des points du cycle semi-disjoint si on en
   *         trouve un, {@code null} sinon.
   */
  private static ArrayList<Point> trouveCycleSemiDisjointRec(Point point, Point pointPrecedent, ArrayList<Point> visites,
      ArrayList<Point> chemin, ArrayList<Point> points) {

    // On marque le point actuel comme visité
    visites.add(point);
    // On l'ajoute au chemin
    chemin.add(point);

    // On récupère les voisins du point
    ArrayList<Point> voisins = Evaluation.neighbor(point, points);

    // On enlève le point précédent dans le parcours, car il sera
    // obligatoirement
    // visité, il ne faut donc pas le prendre en compte.
    voisins.remove(pointPrecedent);

    // Parcours des voisins du point
    for (Point voisin : voisins) {

      // Si le voisin considéré avait déjà été visité précédemment,
      // alors on a trouvé un cycle
      if (visites.contains(voisin)) {
        // On récupère les points du cycle
        ArrayList<Point> cycle = new ArrayList<>();
        for (int i = chemin.lastIndexOf(voisin); i < chemin.size(); ++i) {
          cycle.add(chemin.get(i));
        }
        // Si le cycle correspond, on peut le renvoyer
        if (estSemiDisjoint(cycle, points)) {
          return cycle;
        }
        // Sinon, il faut continuer la recherche avec les voisins suivants
        //
      } else {
        // Appel récursif sur le voisin
        ArrayList<Point> cycleAppel = trouveCycleSemiDisjointRec(voisin, point, visites, chemin, points);
        // Si l'appel récursif a trouvé un cycle semi-disjoint, on le renvoie
        if (cycleAppel != null) {
          return cycleAppel;
        }
      }
    }
    // Pas de cycle semi-disjoint trouvé
    return null;
  }

  /**
   * Vérification qu'un cycle est semi-disjoint
   * 
   * @param cycle
   *          Une {@code ArrayList} contenant les points du cycle à traiter
   * @param points
   *          Les points du graphe
   * @return {@code true} si le cycle passé en paramètre est semi-disjoint,
   *         {@code false} sinon
   */
  private static boolean estSemiDisjoint(ArrayList<Point> cycle, ArrayList<Point> points) {
    // Il faut au moins 3 points pour faire un cycle
    if (cycle.size() < 3) {
      return false;
    }
    // On parcourt l'ensemble des points du cycle
    boolean jokerTrouve = false;
    for (Point p : cycle) {
      ArrayList<Point> voisins = Evaluation.neighbor(p, points);
      if (voisins.size() > 2) {
        // Si on a déjà trouvé un joker, alors on ne peut pas avoir deux points
        // avec
        // un degré de plus de 2.
        if (jokerTrouve) {
          return false;
        }
        // Sinon ce point est le joker
        else {
          jokerTrouve = true;
        }
      }
    }
    return true;

  }

  /**
   * Renvoie le degré du point dans le graphe passé en paramètres.
   * <p>
   * On notera que l'on appelle {@link Evaluation#neighbor(Point, ArrayList)},
   * qui pourrait être potentiellement long sur un graphe de grande taille.
   * </p>
   * 
   * @param point
   *          Le point à traiter
   * @param points
   *          Le graphe considéré
   * @return Le degré de {@code point} dans le graphe représenté par
   *         {@code points}
   */
  private static int degre(Point point, ArrayList<Point> points) {
    // On récupère la liste des voisins de point et on renvoie sa taille.
    return Evaluation.neighbor(point, points).size();
  }

}
